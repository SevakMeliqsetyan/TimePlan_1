package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileDao;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivitySetupProfileBinding;

public class SetupProfileActivity extends AppCompatActivity {

    private ActivitySetupProfileBinding binding;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnContinue.setOnClickListener(v -> onContinueClicked());

        // If profile already exists and Firebase is connected - go straight to main.
        DbProvider.io().execute(() -> {
            UserProfileDao dao = DbProvider.db(this).userProfileDao();
            UserProfileEntity localProfile = dao.getFirstSync();
            String uid = FirebaseAuth.getInstance().getUid();

            if (localProfile != null && uid != null) {
                runOnUiThread(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
                return;
            }

            if (uid != null) {
                // Check Firestore for existing profile
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Profile exists in cloud, restore it
                                String fName = documentSnapshot.getString("firstName");
                                String lName = documentSnapshot.getString("lastName");
                                String city = documentSnapshot.getString("city");
                                
                                if (fName != null && lName != null) {
                                    DbProvider.io().execute(() -> {
                                        DbProvider.db(this).userProfileDao().insert(
                                                new UserProfileEntity(fName, lName, city != null ? city : "", System.currentTimeMillis())
                                        );
                                        restoreSelfCards(uid);
                                    });
                                    return;
                                }
                            }
                            // No profile in cloud, show UI
                        });
            }
        });
    }

    private void restoreSelfCards(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("self_cards").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    DbProvider.io().execute(() -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            CardEntity card = doc.toObject(CardEntity.class);
                            if (card != null) {
                                card.id = 0; // Reset for local DB
                                card.isSelf = true;
                                card.ownerUid = uid;
                                DbProvider.db(this).cardDao().insert(card);
                            }
                        }
                        // After restoring, check if we still need to create defaults
                        ensureDefaultCards(uid);
                        
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        });
                    });
                });
    }

    private void ensureDefaultCards(String uid) {
        // Logic moved from saveProfileAndFinish to be reusable
        var db = DbProvider.db(this);
        UserProfileEntity profile = db.userProfileDao().getFirstSync();
        if (profile == null) return;

        CardEntity personal = db.cardDao().getSelfByTypeSync("PERSONAL");
        if (personal == null) {
            CardEntity personalCard = new CardEntity("PERSONAL", profile.firstName + " " + profile.lastName, System.currentTimeMillis(), true);
            personalCard.ownerUid = uid;
            db.cardDao().insert(personalCard);
            uploadSelfCard(uid, personalCard);
        }

        CardEntity work = db.cardDao().getSelfByTypeSync("WORK");
        if (work == null) {
            CardEntity workCard = new CardEntity("WORK", profile.firstName + " " + profile.lastName, System.currentTimeMillis(), true);
            workCard.firstName = profile.firstName;
            workCard.lastName = profile.lastName;
            workCard.ownerUid = uid;
            workCard.sessionMinutes = 90;
            workCard.workDaysMask = WorkDays.defaultMonToFri();
            db.cardDao().insert(workCard);
            uploadSelfCard(uid, workCard);
            uploadCardToPublic(workCard);
        }
    }

    private void uploadSelfCard(String uid, CardEntity card) {
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("self_cards").document(card.type).set(card);
    }

    private void onContinueClicked() {
        String firstName = binding.inputFirstName.getText() != null
                ? binding.inputFirstName.getText().toString().trim()
                : "";
        String lastName = binding.inputLastName.getText() != null
                ? binding.inputLastName.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(firstName)) {
            binding.inputFirstNameLayout.setError(getString(R.string.enter_name_error));
            return;
        }
        binding.inputFirstNameLayout.setError(null);

        if (TextUtils.isEmpty(lastName)) {
            binding.inputLastNameLayout.setError(getString(R.string.enter_name_error));
            return;
        }
        binding.inputLastNameLayout.setError(null);

        binding.btnContinue.setEnabled(false);

        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Пользователь уже вошел (например, через LoginActivity)
            saveProfileAndFinish(firstName, lastName, currentUser.getUid());
        } else {
            // Пытаемся войти анонимно, если нет пользователя
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveProfileAndFinish(firstName, lastName, FirebaseAuth.getInstance().getUid());
                } else {
                    // Если анонимный вход отключен или произошла ошибка, 
                    // создаем локальный профиль без UID, чтобы приложение хотя бы открылось
                    saveProfileAndFinish(firstName, lastName, null);
                    Toast.makeText(this, "Firebase Auth limited: " + (task.getException() != null ? task.getException().getMessage() : "unknown"), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveProfileAndFinish(String firstName, String lastName, String uid) {
        DbProvider.io().execute(() -> {
            var db = DbProvider.db(this);
            var userDao = db.userProfileDao();
            UserProfileEntity profile = new UserProfileEntity(firstName, lastName, "", System.currentTimeMillis());
            if (userDao.getFirstSync() == null) {
                userDao.insert(profile);
            }
            
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).set(profile);
            }

            // Ensure one personal + one work "self" card exist.
            CardEntity personal = db.cardDao().getSelfByTypeSync("PERSONAL");
            if (personal == null) {
                CardEntity personalCard = new CardEntity(
                        "PERSONAL",
                        firstName + " " + lastName,
                        System.currentTimeMillis(),
                        true
                );
                personalCard.ownerUid = uid;
                db.cardDao().insert(personalCard);
                uploadSelfCard(uid, personalCard);
            } else {
                if (personal.ownerUid == null && uid != null) {
                    personal.ownerUid = uid;
                }
                db.cardDao().update(personal);
                uploadSelfCard(uid, personal);
            }

            CardEntity work = db.cardDao().getSelfByTypeSync("WORK");
            if (work == null) {
                CardEntity workCard = new CardEntity(
                        "WORK",
                        firstName + " " + lastName,
                        System.currentTimeMillis(),
                        true
                );
                workCard.firstName = firstName;
                workCard.lastName = lastName;
                workCard.ownerUid = uid;
                workCard.sessionMinutes = 90;
                workCard.workDaysMask = WorkDays.defaultMonToFri();
                db.cardDao().insert(workCard);
                if (uid != null) {
                    uploadSelfCard(uid, workCard);
                    uploadCardToPublic(workCard);
                }
            } else {
                if (work.ownerUid == null && uid != null) {
                    work.ownerUid = uid;
                }
                work.firstName = firstName;
                work.lastName = lastName;
                db.cardDao().update(work);
                if (work.ownerUid != null) {
                    uploadSelfCard(uid, work);
                    uploadCardToPublic(work);
                }
            }

            runOnUiThread(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }

    private void uploadCardToPublic(CardEntity card) {
        if (card.ownerUid == null) return;
        FirebaseFirestore.getInstance().collection("users_public_cards")
                .document(card.ownerUid)
                .set(card);
    }
}
