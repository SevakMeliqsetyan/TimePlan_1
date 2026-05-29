package sevak.meliqsetyan.samsung_project_2;

import sevak.meliqsetyan.samsung_project_2.databinding.ActivityLoginBinding;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
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

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();

        // ПРОВЕРКА: если пользователь уже вошел, сразу переходим на MainActivity
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Кнопка для тестового пользователя
        binding.btnTestUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.etEmail.setText("innovationcampus26@gmail.com");
                binding.etPassword.setText("Samsung2026");
            }
        });

        // Переход на регистрацию
        binding.btnRegisterRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Вход через Firebase
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    DbProvider.clearDatabase(LoginActivity.this);
                                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, getString(R.string.error_prefix, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}