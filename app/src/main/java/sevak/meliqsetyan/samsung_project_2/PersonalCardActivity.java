package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.LocalDate;
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalCardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Исправлено: корректная обработка системных отступов (Edge-to-Edge)
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
            DbProvider.io().execute(() -> DbProvider.db(this).personalTaskDao().deleteById(task.id));
        });
        binding.tasksList.setLayoutManager(new LinearLayoutManager(this));
        binding.tasksList.setAdapter(tasksAdapter);

        setupCalendar();
        observeSelectedDay();

        binding.btnAddTask.setOnClickListener(v -> showAddTaskDialog());

        DbProvider.db(this).cardDao().observeById(cardId).observe(this, card -> {
            if (card != null) {
                binding.toolbar.setTitle(card.title);
            }
        });
    }

    private void setupCalendar() {
        // Устанавливаем текущий день по умолчанию
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Настройка стандартного CalendarView
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Месяц в CalendarView начинается с 0, поэтому прибавляем 1
            selectedEpochDay = LocalDate.of(year, month + 1, dayOfMonth).toEpochDay();
            binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
            observeSelectedDay();
        });
    }

    private void observeSelectedDay() {
        if (tasksLiveData != null) {
            tasksLiveData.removeObservers(this);
        }
        tasksLiveData = DbProvider.db(this).personalTaskDao().observeForDay(cardId, selectedEpochDay);
        tasksLiveData.observe(this, tasks -> {
            tasksAdapter.submitList(tasks);
            boolean empty = tasks == null || tasks.isEmpty();
            binding.emptyTasks.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddTaskDialog() {
        AddPersonalTaskDialog.show(getSupportFragmentManager(), (timeMinutes, title) -> {
            if (TextUtils.isEmpty(title)) return;
            DbProvider.io().execute(() -> DbProvider.db(this).personalTaskDao().insert(
                    new PersonalTaskEntity(cardId, selectedEpochDay, timeMinutes, title)
            ));
        });
    }
}