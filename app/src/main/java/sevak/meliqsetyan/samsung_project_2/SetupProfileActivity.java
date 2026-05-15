package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

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
            UserProfileEntity first = dao.getFirstSync();
            if (first != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                runOnUiThread(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            }
        });
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
        if (TextUtils.isEmpty(lastName)) {
            binding.inputLastNameLayout.setError(getString(R.string.enter_name_error));
            return;
        }

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
            if (userDao.getFirstSync() == null) {
                userDao.insert(new UserProfileEntity(firstName, lastName, System.currentTimeMillis()));
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
            } else if (personal.ownerUid == null && uid != null) {
                personal.ownerUid = uid;
                db.cardDao().update(personal);
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
            } else if (work.ownerUid == null && uid != null) {
                work.ownerUid = uid;
                db.cardDao().update(work);
            }

            runOnUiThread(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }
}
