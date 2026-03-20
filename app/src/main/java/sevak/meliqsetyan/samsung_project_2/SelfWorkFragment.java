package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView; // Используем стандартный

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.LiveData;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.WorkBookingEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityWorkCardBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class SelfWorkFragment extends Fragment {

    private ActivityWorkCardBinding binding;

    private long cardId = -1;
    private long selectedEpochDay;

    private int sessionMinutes = 90;
    private int workDaysMask = WorkDays.defaultMonToFri();
    private int workStartMinutes = 9 * 60;
    private int workEndMinutes = 20 * 60;
    private Integer breakStartMinutes = null;
    private Integer breakEndMinutes = null;

    private WorkSlotsAdapter slotsAdapter;
    private LiveData<List<WorkBookingEntity>> bookingsLiveData;

    // ПЕРЕМЕННЫЕ СЛОЖНОГО КАЛЕНДАРЯ УДАЛЕНЫ

    private boolean profileInitialized = false;
    private boolean chipsUpdating = false;

    private static final int DAY_START_MIN = 9 * 60;
    private static final int DAY_END_MIN = 20 * 60;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityWorkCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setTitle("");

        slotsAdapter = new WorkSlotsAdapter(slot -> EditWorkSlotDialog.show(
                getParentFragmentManager(),
                TimeUtils.formatTimeMinutes(slot.startMinutes),
                slot.busy ? slot.clientName : "",
                clientName -> saveSlot(slot.startMinutes, clientName)
        ));
        binding.slotsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.slotsList.setAdapter(slotsAdapter);

        setupCalendar();
        setupDaysChips();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

        observeCard();
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Настройка СТАНДАРТНОГО CalendarView
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // Устанавливаем минимальную и максимальную дату
        binding.calendarView.setMinDate(cal.getTimeInMillis());
        cal.add(java.util.Calendar.MONTH, 6);
        binding.calendarView.setMaxDate(cal.getTimeInMillis());

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Месяц в CalendarView начинается с 0, прибавляем 1 для LocalDate
            selectedEpochDay = LocalDate.of(year, month + 1, dayOfMonth).toEpochDay();
            binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
            observeSelectedDayBookings();
        });
    }

    private void setupDaysChips() {
        binding.chipMon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.MON, isChecked);
        });
        binding.chipTue.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.TUE, isChecked);
        });
        binding.chipWed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.WED, isChecked);
        });
        binding.chipThu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.THU, isChecked);
        });
        binding.chipFri.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.FRI, isChecked);
        });
        binding.chipSat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.SAT, isChecked);
        });
        binding.chipSun.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (chipsUpdating) return;
            workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.SUN, isChecked);
        });
    }

    private void applyDaysMaskToChips() {
        chipsUpdating = true;
        binding.chipMon.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.MON));
        binding.chipTue.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.TUE));
        binding.chipWed.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.WED));
        binding.chipThu.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.THU));
        binding.chipFri.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.FRI));
        binding.chipSat.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.SAT));
        binding.chipSun.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.SUN));
        chipsUpdating = false;
    }

    private void observeCard() {
        DbProvider.db(requireContext()).cardDao().observeSelfByType("WORK")
                .observe(getViewLifecycleOwner(), this::onCardChanged);
    }

    private void onCardChanged(CardEntity card) {
        if (card == null) return;
        cardId = card.id;
        binding.toolbar.setTitle(card.title);

        boolean firstTime = !profileInitialized;
        if (firstTime) {
            profileInitialized = true;
            binding.inputFirstName.setText(card.firstName != null ? card.firstName : "");
            binding.inputLastName.setText(card.lastName != null ? card.lastName : "");
            binding.inputAge.setText(card.age != null ? String.valueOf(card.age) : "");
            binding.inputProfession.setText(card.profession != null ? card.profession : "");
        }

        if (card.sessionMinutes > 0 && card.sessionMinutes != sessionMinutes) {
            sessionMinutes = card.sessionMinutes;
        }
        if (card.workDaysMask != 0 && card.workDaysMask != workDaysMask) {
            workDaysMask = card.workDaysMask;
        }

        if (card.workStartMinutes > 0) {
            workStartMinutes = card.workStartMinutes;
        }
        if (card.workEndMinutes > 0) {
            workEndMinutes = card.workEndMinutes;
        }
        breakStartMinutes = card.breakStartMinutes;
        breakEndMinutes = card.breakEndMinutes;

        binding.inputSessionMinutes.setText(String.valueOf(sessionMinutes));
        binding.inputWorkStart.setText(TimeUtils.formatTimeMinutes(workStartMinutes));
        binding.inputWorkEnd.setText(TimeUtils.formatTimeMinutes(workEndMinutes));
        binding.inputBreakStart.setText(breakStartMinutes != null ? TimeUtils.formatTimeMinutes(breakStartMinutes) : "");
        binding.inputBreakEnd.setText(breakEndMinutes != null ? TimeUtils.formatTimeMinutes(breakEndMinutes) : "");
        applyDaysMaskToChips();

        observeSelectedDayBookings();
    }

    private void observeSelectedDayBookings() {
        if (cardId <= 0) return;

        if (bookingsLiveData != null) {
            bookingsLiveData.removeObservers(getViewLifecycleOwner());
        }

        bookingsLiveData = DbProvider.db(requireContext()).workBookingDao().observeForDay(cardId, selectedEpochDay);
        bookingsLiveData.observe(getViewLifecycleOwner(), bookings -> {
            if (!isWorkDay(selectedEpochDay)) {
                binding.dayHint.setText("Нерабочий день");
                slotsAdapter.submitList(new ArrayList<>());
                return;
            }
            binding.dayHint.setText("График работы");

            Map<Integer, WorkBookingEntity> byStart = new HashMap<>();
            if (bookings != null) {
                for (WorkBookingEntity b : bookings) {
                    byStart.put(b.startMinutes, b);
                }
            }

            List<Integer> starts = computeSlotStarts();
            List<WorkSlotsAdapter.WorkSlotUi> ui = new ArrayList<>(starts.size());
            for (int start : starts) {
                WorkBookingEntity b = byStart.get(start);
                boolean busy = b != null && !TextUtils.isEmpty(b.clientName);
                ui.add(new WorkSlotsAdapter.WorkSlotUi(start, busy, busy ? b.clientName : null));
            }
            slotsAdapter.submitList(ui);
        });
    }

    private void saveProfile() {
        String firstName = textOrNull(getText(binding.inputFirstName));
        String lastName = textOrNull(getText(binding.inputLastName));
        String profession = textOrNull(getText(binding.inputProfession));

        Integer age = null;
        String ageStr = getText(binding.inputAge);
        if (!TextUtils.isEmpty(ageStr)) {
            try { age = Integer.parseInt(ageStr); } catch (Exception ignored) { }
        }

        int session = sessionMinutes;
        String sessionStr = getText(binding.inputSessionMinutes);
        if (!TextUtils.isEmpty(sessionStr)) {
            try { session = Math.max(15, Integer.parseInt(sessionStr)); } catch (Exception ignored) { }
        }
        sessionMinutes = session;

        int parsedStart = TimeUtils.parseTimeToMinutes(getText(binding.inputWorkStart));
        int parsedEnd = TimeUtils.parseTimeToMinutes(getText(binding.inputWorkEnd));
        if (parsedStart >= 0) workStartMinutes = parsedStart;
        if (parsedEnd >= 0) workEndMinutes = parsedEnd;

        int parsedBreakStart = TimeUtils.parseTimeToMinutes(getText(binding.inputBreakStart));
        int parsedBreakEnd = TimeUtils.parseTimeToMinutes(getText(binding.inputBreakEnd));
        breakStartMinutes = parsedBreakStart >= 0 ? parsedBreakStart : null;
        breakEndMinutes = parsedBreakEnd >= 0 ? parsedBreakEnd : null;

        int finalSession = sessionMinutes;
        Integer finalAge = age;
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(requireContext()).cardDao().getSelfByTypeSync("WORK");
            if (card == null) return;
            card.firstName = firstName;
            card.lastName = lastName;
            card.profession = profession;
            card.age = finalAge;
            card.sessionMinutes = finalSession;
            card.workDaysMask = workDaysMask;
            card.workStartMinutes = workStartMinutes;
            card.workEndMinutes = workEndMinutes;
            card.breakStartMinutes = breakStartMinutes;
            card.breakEndMinutes = breakEndMinutes;
            DbProvider.db(requireContext()).cardDao().update(card);
        });

        observeSelectedDayBookings();
    }

    private void saveSlot(int startMinutes, String clientName) {
        String name = clientName != null ? clientName.trim() : "";
        if (cardId <= 0) return;

        DbProvider.io().execute(() -> {
            if (TextUtils.isEmpty(name)) {
                DbProvider.db(requireContext()).workBookingDao().deleteSlot(cardId, selectedEpochDay, startMinutes);
            } else {
                DbProvider.db(requireContext()).workBookingDao().insert(new WorkBookingEntity(cardId, selectedEpochDay, startMinutes, name));
            }
        });
    }

    private static String getText(android.widget.TextView view) {
        return view.getText() != null ? view.getText().toString().trim() : "";
    }

    private static String textOrNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private List<Integer> computeSlotStarts() {
        int step = Math.max(15, sessionMinutes);
        int start = Math.max(0, workStartMinutes);
        int end = Math.max(start + step, workEndMinutes);

        List<Integer> starts = new ArrayList<>();
        for (int slotStart = start; slotStart + step <= end; slotStart += step) {
            if (breakStartMinutes != null && breakEndMinutes != null) {
                int bStart = Math.min(breakStartMinutes, breakEndMinutes);
                int bEnd = Math.max(breakStartMinutes, breakEndMinutes);
                int slotEnd = slotStart + step;
                boolean overlaps = slotStart < bEnd && slotEnd > bStart;
                if (overlaps) continue;
            }
            starts.add(slotStart);
        }
        return starts;
    }

    private boolean isWorkDay(long epochDay) {
        LocalDate d = LocalDate.ofEpochDay(epochDay);
        int dayOfWeek = d.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
        int bit = 1 << (dayOfWeek - 1);
        return (workDaysMask & bit) != 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}