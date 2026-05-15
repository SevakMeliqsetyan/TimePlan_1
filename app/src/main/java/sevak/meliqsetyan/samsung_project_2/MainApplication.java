package sevak.meliqsetyan.samsung_project_2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class MainApplication extends Application {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";

    @Override
    public void onCreate() {
        super.onCreate();
        applySettings();
    }

    private void applySettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Apply Theme (Default Dark)
        int themeMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // Language is handled in attachBaseContext but we can ensure it here too for some cases
    }

    @Override
    protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANGUAGE, "en");
        super.attachBaseContext(updateLocale(base, lang));
    }

    public static Context updateLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public void setLanguage(String lang) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, lang)
                .apply();
    }

    public void setThemeMode(int mode) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
