package sevak.meliqsetyan.samsung_project_2;

import sevak.meliqsetyan.samsung_project_2.databinding.ActivityRegisterBinding;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // Кнопка для тестового пользователя
        binding.btnTestUser.setOnClickListener(v -> {
            binding.etEmail.setText("innovationcampus26@gmail.com");
            binding.etPassword.setText("Samsung2026");
        });

        // Кнопка для перехода обратно на логин
        binding.btnLoginRedirect.setOnClickListener(view -> {
            finish(); // Просто закрываем текущую активность
        });

        binding.btnRegister.setOnClickListener(view -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(RegisterActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, task -> {
                        if(task.isSuccessful()){
                            com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Регистрация успешна. Проверьте почту для подтверждения.", Toast.LENGTH_LONG).show();
                                                auth.signOut(); // Выходим, чтобы пользователь подтвердил почту перед входом
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Ошибка отправки письма: " + verifyTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, getString(R.string.error_prefix, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}