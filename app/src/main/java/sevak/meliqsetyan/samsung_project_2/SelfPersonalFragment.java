package sevak.meliqsetyan.samsung_project_2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView; // Стандартный Android календарь

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.LiveData;

import java.util.Calendar;
import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.PersonalTaskEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityPersonalCardBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SelfPersonalFragment extends Fragment {

    private ActivityPersonalCardBinding binding;
    private long cardId = -1;
    private long selectedEpochDay;
    private PersonalTasksAdapter tasksAdapter;
    private LiveData<List<PersonalTaskEntity>> tasksLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityPersonalCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_close) {
                // Переключаемся на вкладку Расписания (Work)
                if (getActivity() instanceof MainActivity) {
                    com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_work);
                }
                return true;
            }
            return false;
        });

        tasksAdapter = new PersonalTasksAdapter(task -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_task_title)
                    .setMessage(getString(R.string.delete_record_message, task.title))
                    .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                        DbProvider.io().execute(() -> {
                            DbProvider.db(requireContext()).personalTaskDao().deleteById(task.id);
                            cancelNotification(task.id);
                        });
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });

        binding.tasksList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tasksList.setAdapter(tasksAdapter);

        binding.btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        setupCalendar();
        observeCardAndSelectedDay();
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Работаем со стандартным CalendarView
        binding.calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            selectedEpochDay = TimeUtils.toEpochDay(cal);
            binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
            observeSelectedDay();
        });
    }

    private void observeCardAndSelectedDay() {
        DbProvider.db(requireContext()).cardDao().observeSelfByType("PERSONAL")
                .observe(getViewLifecycleOwner(), card -> {
                    if (card == null) return;
                    cardId = card.id;
                    binding.toolbar.setTitle(card.title);
                    observeSelectedDay();
                });
    }

    private void observeSelectedDay() {
        if (cardId <= 0) return;
        if (tasksLiveData != null) tasksLiveData.removeObservers(getViewLifecycleOwner());

        tasksLiveData = DbProvider.db(requireContext()).personalTaskDao().observeForDay(cardId, selectedEpochDay);
        tasksLiveData.observe(getViewLifecycleOwner(), tasks -> {
            tasksAdapter.submitList(tasks);
            binding.emptyTasks.setVisibility(tasks == null || tasks.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddTaskDialog() {
        if (cardId <= 0) return;
        AddPersonalTaskDialog.show(getParentFragmentManager(), (timeMinutes, title, reminderBeforeMinutes) -> {
            if (TextUtils.isEmpty(title)) return;
            DbProvider.io().execute(() -> {
                long id = DbProvider.db(requireContext()).personalTaskDao().insert(
                        new PersonalTaskEntity(cardId, selectedEpochDay, timeMinutes, title, reminderBeforeMinutes)
                );
                // Schedule notification here
                scheduleNotification(id, selectedEpochDay, timeMinutes, title, reminderBeforeMinutes);
            });
        });
    }

    private void scheduleNotification(long taskId, long epochDay, int timeMinutes, String title, int reminderBeforeMinutes) {
        if (reminderBeforeMinutes < 0) return;

        long taskTimeMillis = TimeUtils.getLocalMillis(epochDay, timeMinutes);
        long triggerAtMillis = taskTimeMillis - (reminderBeforeMinutes * 60000L);
        
        // Don't schedule if time is in the past
        if (triggerAtMillis < System.currentTimeMillis()) return;

        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        intent.putExtra(TaskNotificationReceiver.EXTRA_TASK_TITLE, title);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                (int) taskId, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    private void cancelNotification(long taskId) {
        Context context = getContext();
        if (context == null) return;
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                (int) taskId, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
        );

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}