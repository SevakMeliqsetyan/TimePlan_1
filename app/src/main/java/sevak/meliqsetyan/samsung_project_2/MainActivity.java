package sevak.meliqsetyan.samsung_project_2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent;
import android.content.Context;

import java.util.List;

import sevak.meliqsetyan.samsung_project_2.databinding.ActivityMainBinding;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Fragment personalFragment;
    private Fragment workFragment;
    private Fragment connectionsFragment;
    private ListenerRegistration firestoreListener;

    public static final String CHANNEL_REQUESTS_ID = "contact_requests";
    public static final String EXTRA_OPEN_CONNECTIONS = "open_connections";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Разрешите уведомления в настройках для получения напоминаний", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Создаем канал уведомлений
        createNotificationChannel();
        
        // Запрашиваем разрешение (Android 13+)
        checkNotificationPermission();

        ensureFirebaseAndLoad(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_CONNECTIONS, false)) {
            if (connectionsFragment == null) {
                connectionsFragment = new ConnectionsFragment();
            }
            switchToFragment(connectionsFragment);
            if (binding != null) {
                binding.bottomNavigation.setSelectedItemId(R.id.nav_connections);
            }
        }
    }

    private void ensureFirebaseAndLoad(Bundle savedInstanceState) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Если пользователь не вошел, отправляем на LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        checkProfileAndStart(savedInstanceState);
    }

    private void checkProfileAndStart(Bundle savedInstanceState) {
        DbProvider.io().execute(() -> {
            UserProfileEntity profile = DbProvider.db(this).userProfileDao().getFirstSync();
            
            // Ensure self cards have ownerUid of current user
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                java.util.List<CardEntity> selfCards = DbProvider.db(this).cardDao().getSelfCardsSync();
                for (CardEntity c : selfCards) {
                    if (!uid.equals(c.ownerUid)) {
                        c.ownerUid = uid;
                        DbProvider.db(this).cardDao().update(c);
                    }
                }
            }

            runOnUiThread(() -> {
                if (profile == null) {
                    startActivity(new Intent(this, SetupProfileActivity.class));
                    finish();
                    return;
                }
                setupFragments(savedInstanceState);
                setupBottomNavigation();
                
                // Если пришли из уведомления, открываем контакты
                if (getIntent().getBooleanExtra(EXTRA_OPEN_CONNECTIONS, false)) {
                    handleIntent(getIntent());
                }

                startListeningForIncomingCards();
            });
        });
    }

    private void startListeningForIncomingCards() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        if (firestoreListener != null) return;

        firestoreListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(myUid)
                .collection("incoming_cards")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value == null || value.isEmpty()) return;

                    for (QueryDocumentSnapshot doc : value) {
                        CardEntity receivedCard = doc.toObject(CardEntity.class);
                        if (receivedCard == null || receivedCard.type == null || receivedCard.title == null) {
                            doc.getReference().delete();
                            continue;
                        }

                        receivedCard.isSelf = false;
                        receivedCard.createdAtEpochMs = System.currentTimeMillis();

                        DbProvider.io().execute(() -> {
                            try {
                                if (receivedCard.ownerUid != null) {
                                    CardEntity existing = DbProvider.db(this).cardDao().getByOwnerUidSync(receivedCard.ownerUid);
                                    if (existing != null) {
                                        receivedCard.id = existing.id;
                                        DbProvider.db(this).cardDao().update(receivedCard);
                                        // Clean old experience
                                        DbProvider.db(this).workExperienceDao().deleteByCardId(existing.id);
                                    } else {
                                        receivedCard.id = DbProvider.db(this).cardDao().insert(receivedCard);
                                    }
                                    
                                    // Save received experience
                                    if (receivedCard.experienceList != null && receivedCard.id > 0) {
                                        for (sevak.meliqsetyan.samsung_project_2.data.db.WorkExperienceEntity exp : receivedCard.experienceList) {
                                            exp.id = 0;
                                            exp.cardId = receivedCard.id;
                                            DbProvider.db(this).workExperienceDao().insert(exp);
                                        }
                                    }
                                } else {
                                    DbProvider.db(this).cardDao().insert(receivedCard);
                                }
                                runOnUiThread(() -> showIncomingCardNotification(receivedCard));
                            } catch (Exception e) {
                                android.util.Log.e("Firebase", "Error saving received card", e);
                            } finally {
                                doc.getReference().delete();
                            }
                        });
                    }
                });
    }

    private void showIncomingCardNotification(CardEntity card) {
        String senderName = (card.firstName != null ? card.firstName : "") + " " + (card.lastName != null ? card.lastName : "");
        if (senderName.trim().isEmpty()) senderName = getString(R.string.new_contact_default);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_OPEN_CONNECTIONS, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_REQUESTS_ID)
                .setSmallIcon(R.drawable.ic_nav_people)
                .setContentTitle(getString(R.string.new_card_notification_title))
                .setContentText(getString(R.string.new_card_notification_message, senderName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Канал для задач
            NotificationChannel taskChannel = new NotificationChannel(
                    TaskNotificationReceiver.CHANNEL_ID,
                    getString(R.string.channel_tasks_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            taskChannel.setDescription(getString(R.string.channel_tasks_desc));
            manager.createNotificationChannel(taskChannel);

            // Канал для визиток
            NotificationChannel requestChannel = new NotificationChannel(
                    CHANNEL_REQUESTS_ID,
                    getString(R.string.channel_requests_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            requestChannel.setDescription(getString(R.string.channel_requests_desc));
            manager.createNotificationChannel(requestChannel);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            personalFragment = new SelfPersonalFragment();
            workFragment = new SelfWorkFragment();
            connectionsFragment = new ConnectionsFragment();
            switchToFragment(workFragment);
        } else {
            personalFragment = getSupportFragmentManager().findFragmentByTag("personal");
            workFragment = getSupportFragmentManager().findFragmentByTag("work");
            connectionsFragment = getSupportFragmentManager().findFragmentByTag("connections");
            if (personalFragment == null) personalFragment = new SelfPersonalFragment();
            if (workFragment == null) workFragment = new SelfWorkFragment();
            if (connectionsFragment == null) connectionsFragment = new ConnectionsFragment();
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_work) { switchToFragment(workFragment); return true; }
            if (itemId == R.id.nav_personal) { switchToFragment(personalFragment); return true; }
            if (itemId == R.id.nav_connections) { switchToFragment(connectionsFragment); return true; }
            return false;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_work);
    }

    private void switchToFragment(Fragment fragment) {
        String tag = (fragment instanceof SelfWorkFragment) ? "work" : 
                    (fragment instanceof SelfPersonalFragment) ? "personal" : "connections";
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }
}
