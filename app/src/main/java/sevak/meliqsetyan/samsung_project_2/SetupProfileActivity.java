package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileDao;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivitySetupProfileBinding;

public class SetupProfileActivity extends AppCompatActivity {

    private ActivitySetupProfileBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnContinue.setOnClickListener(v -> onContinueClicked());

        // If profile already exists - go straight to main.
        DbProvider.io().execute(() -> {
            UserProfileDao dao = DbProvider.db(this).userProfileDao();
            UserProfileEntity first = dao.getFirstSync();
            if (first != null) {
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
            binding.inputFirstNameLayout.setError("Введите имя");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            binding.inputLastNameLayout.setError("Введите фамилию");
            return;
        }

        binding.btnContinue.setEnabled(false);

        DbProvider.io().execute(() -> {
            var db = DbProvider.db(this);
            var userDao = db.userProfileDao();
            if (userDao.getFirstSync() == null) {
                userDao.insert(new UserProfileEntity(firstName, lastName, System.currentTimeMillis()));
            }

            // Ensure one personal + one work "self" card exist.
            CardEntity personal = db.cardDao().getSelfByTypeSync("PERSONAL");
            if (personal == null) {
                db.cardDao().insert(new CardEntity(
                        "PERSONAL",
                        firstName + " " + lastName,
                        System.currentTimeMillis(),
                        true
                ));
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
                workCard.age = null;
                workCard.profession = null;
                workCard.sessionMinutes = 90;
                workCard.workDaysMask = WorkDays.defaultMonToFri();
                db.cardDao().insert(workCard);
            }

            runOnUiThread(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        });
    }
}

