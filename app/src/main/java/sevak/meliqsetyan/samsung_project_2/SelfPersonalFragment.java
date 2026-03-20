package sevak.meliqsetyan.samsung_project_2;

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

import java.time.LocalDate;
import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.PersonalTaskEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityPersonalCardBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

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

        tasksAdapter = new PersonalTasksAdapter(task -> DbProvider.io().execute(() -> {
            DbProvider.db(requireContext()).personalTaskDao().deleteById(task.id);
        }));

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
            // Месяц в CalendarView начинается с 0
            selectedEpochDay = LocalDate.of(year, month + 1, dayOfMonth).toEpochDay();
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
        AddPersonalTaskDialog.show(getParentFragmentManager(), (timeMinutes, title) -> {
            if (TextUtils.isEmpty(title)) return;
            DbProvider.io().execute(() -> DbProvider.db(requireContext()).personalTaskDao().insert(
                    new PersonalTaskEntity(cardId, selectedEpochDay, timeMinutes, title)
            ));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}