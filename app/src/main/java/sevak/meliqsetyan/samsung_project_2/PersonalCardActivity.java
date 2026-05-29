package sevak.meliqsetyan.samsung_project_2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.PersonalTaskEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityPersonalCardBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class PersonalCardActivity extends AppCompatActivity {

    public static final String EXTRA_CARD_ID = "extra_card_id";
    private ActivityPersonalCardBinding binding;
    private long cardId;
    private long selectedEpochDay;
    private PersonalTasksAdapter tasksAdapter;
    private LiveData<List<PersonalTaskEntity>> tasksLiveData;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalCardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.toolbar.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        cardId = getIntent().getLongExtra(EXTRA_CARD_ID, -1);
        if (cardId <= 0) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        tasksAdapter = new PersonalTasksAdapter(task -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_task_title)
                    .setMessage(getString(R.string.delete_record_message, task.title))
                    .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                        cancelNotification(task);
                        DbProvider.io().execute(() -> DbProvider.db(this).personalTaskDao().deleteById(task.id));
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });
        binding.tasksList.setLayoutManager(new LinearLayoutManager(this));
        binding.tasksList.setAdapter(tasksAdapter);

        setupCalendar();
        observeSelectedDay();

        binding.btnAddTask.setOnClickListener(v -> {
            if (selectedEpochDay < TimeUtils.todayEpochDay()) {
                Toast.makeText(this, R.string.past_day_edit_error, Toast.LENGTH_SHORT).show();
            } else {
                checkAlarmPermissionAndShowDialog();
            }
        });

        DbProvider.db(this).cardDao().observeById(cardId).observe(this, card -> {
            if (card != null) binding.toolbar.setTitle(card.title);
        });
    }

    private void checkAlarmPermissionAndShowDialog() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Разрешение на напоминания")
                    .setMessage("Пожалуйста, разрешите установку точных будильников в настройках системы, чтобы уведомления приходили вовремя.")
                    .setPositiveButton("Настройки", (d, w) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.open_settings_error, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, null).show();
        } else {
            showAddTaskDialog();
        }
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
        binding.calendarView.setMinDate(System.currentTimeMillis() - 1000);
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            selectedEpochDay = TimeUtils.toEpochDay(cal);
            binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
            updateAddButtonVisibility();
            observeSelectedDay();
        });
        updateAddButtonVisibility();
    }

    private void updateAddButtonVisibility() {
        binding.btnAddTask.setAlpha(selectedEpochDay < TimeUtils.todayEpochDay() ? 0.4f : 1.0f);
    }

    private void observeSelectedDay() {
        if (tasksLiveData != null) tasksLiveData.removeObservers(this);
        tasksLiveData = DbProvider.db(this).personalTaskDao().observeForDay(cardId, selectedEpochDay);
        tasksLiveData.observe(this, tasks -> {
            tasksAdapter.submitList(tasks);
            binding.emptyTasks.setVisibility(tasks == null || tasks.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddTaskDialog() {
        AddPersonalTaskDialog.show(getSupportFragmentManager(), (timeMinutes, title, reminderBeforeMinutes) -> {
            if (TextUtils.isEmpty(title)) return;
            DbProvider.io().execute(() -> {
                PersonalTaskEntity task = new PersonalTaskEntity(cardId, selectedEpochDay, timeMinutes, title, reminderBeforeMinutes);
                long id = DbProvider.db(this).personalTaskDao().insert(task);
                task.id = id;
                scheduleNotification(task);
            });
        });
    }

    private void scheduleNotification(PersonalTaskEntity task) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        intent.putExtra(TaskNotificationReceiver.EXTRA_TASK_TITLE, task.title);
        intent.setData(android.net.Uri.parse("task://" + task.id + "_" + System.currentTimeMillis()));

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        } else {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(this, (int) task.id, intent, flags);

        long taskTimeMillis = TimeUtils.getLocalMillis(task.dateEpochDay, task.timeMinutes);
        long notifyTime = taskTimeMillis - ((long) task.reminderBeforeMinutes * 60 * 1000);

        if (notifyTime <= System.currentTimeMillis()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, notifyTime, pi);
        }
    }

    private void cancelNotification(PersonalTaskEntity task) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        intent.setData(android.net.Uri.parse("task://" + task.id));
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) task.id, intent, 
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (am != null && pi != null) am.cancel(pi);
    }
}