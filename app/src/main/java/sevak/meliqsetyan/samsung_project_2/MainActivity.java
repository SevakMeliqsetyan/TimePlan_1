package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Включаем EdgeToEdge для современного вида
        EdgeToEdge.enable(this);

        // Инициализация ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Настройка отступов для системных панелей (статус-бар и навигация)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load profile from DB; if it's missing - ask user once.
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
            if (itemId == R.id.nav_work) {
                switchToFragment(workFragment);
                return true;
            } else if (itemId == R.id.nav_personal) {
                switchToFragment(personalFragment);
                return true;
            } else if (itemId == R.id.nav_connections) {
                switchToFragment(connectionsFragment);
                return true;
            }
            return false;
        });

        // Select default tab for the first launch.
        binding.bottomNavigation.setSelectedItemId(R.id.nav_work);
    }

    private void switchToFragment(Fragment fragment) {
        String tag;
        if (fragment instanceof SelfWorkFragment) {
            tag = "work";
        } else if (fragment instanceof SelfPersonalFragment) {
            tag = "personal";
        } else {
            tag = "connections";
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Плавная анимация переключения
        transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
        );

        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }
}