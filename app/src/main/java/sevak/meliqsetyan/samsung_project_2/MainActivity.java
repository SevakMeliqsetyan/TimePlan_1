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

import sevak.meliqsetyan.samsung_project_2.databinding.ActivityMainBinding;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileEntity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Fragment personalFragment;
    private Fragment workFragment;
    private Fragment connectionsFragment;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Разрешите уведомления в настройках для получения напоминаний", Toast.LENGTH_LONG).show();
                }
            });

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

        DbProvider.io().execute(() -> {
            UserProfileEntity profile = DbProvider.db(this).userProfileDao().getFirstSync();
            runOnUiThread(() -> {
                if (profile == null) {
                    startActivity(new Intent(this, SetupProfileActivity.class));
                    finish();
                    return;
                }
                setupFragments(savedInstanceState);
                setupBottomNavigation();
            });
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    TaskNotificationReceiver.CHANNEL_ID,
                    "Напоминания о задачах",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Используется для напоминаний о личных делах");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
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