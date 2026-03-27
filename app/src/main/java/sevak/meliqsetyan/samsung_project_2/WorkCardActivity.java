package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.LocalDate;
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

public class WorkCardActivity extends AppCompatActivity {

    public static final String EXTRA_CARD_ID = "extra_card_id";

    private static final int DAY_START_MIN = 9 * 60;
    private static final int DAY_END_MIN = 20 * 60;

    private ActivityWorkCardBinding binding;
    private long cardId;
    private long selectedEpochDay;

    private int sessionMinutes = 90;
    private int workDaysMask = WorkDays.defaultMonToFri();

    private WorkSlotsAdapter slotsAdapter;
    private LiveData<List<WorkBookingEntity>> bookingsLiveData;

    private boolean profileInitialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkCardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            binding.appBar.setPadding(0, top, 0, 0);
            return insets;
        });

        cardId = getIntent().getLongExtra(EXTRA_CARD_ID, -1);
        if (cardId <= 0) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        slotsAdapter = new WorkSlotsAdapter(slot -> {
            if (selectedEpochDay < TimeUtils.todayEpochDay()) {
                Toast.makeText(this, "Нельзя редактировать записи в прошедших днях", Toast.LENGTH_SHORT).show();
                return;
            }
            EditWorkSlotDialog.show(
                    getSupportFragmentManager(),
                    TimeUtils.formatTimeMinutes(slot.startMinutes),
                    slot.busy ? slot.clientName : "",
                    clientName -> saveSlot(slot.startMinutes, clientName)
            );
        });
        binding.slotsList.setLayoutManager(new LinearLayoutManager(this));
        binding.slotsList.setAdapter(slotsAdapter);

        setupCalendar();
        setupDaysChips();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

        DbProvider.db(this).cardDao().observeById(cardId).observe(this, card -> {
            if (card == null) return;
            binding.toolbar.setTitle(card.title);

            if (!profileInitialized) {
                profileInitialized = true;
                binding.inputFirstName.setText(card.firstName != null ? card.firstName : "");
                binding.inputLastName.setText(card.lastName != null ? card.lastName : "");
                binding.inputAge.setText(card.age != null ? String.valueOf(card.age) : "");
                binding.inputProfession.setText(card.profession != null ? card.profession : "");
                sessionMinutes = card.sessionMinutes > 0 ? card.sessionMinutes : 90;
                workDaysMask = card.workDaysMask != 0 ? card.workDaysMask : WorkDays.defaultMonToFri();
                binding.inputSessionMinutes.setText(String.valueOf(sessionMinutes));
                applyDaysMaskToChips();
            } else {
                if (card.sessionMinutes > 0 && card.sessionMinutes != sessionMinutes) {
                    sessionMinutes = card.sessionMinutes;
                    binding.inputSessionMinutes.setText(String.valueOf(sessionMinutes));
                }
                if (card.workDaysMask != 0 && card.workDaysMask != workDaysMask) {
                    workDaysMask = card.workDaysMask;
                    applyDaysMaskToChips();
                }
            }
            observeSelectedDayBookings();
        });
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Ограничиваем выбор в календаре (визуально)
        binding.calendarView.setMinDate(System.currentTimeMillis() - 1000);

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedEpochDay = LocalDate.of(year, month + 1, dayOfMonth).toEpochDay();
            binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));
            observeSelectedDayBookings();
        });
    }

    private void setupDaysChips() {
        binding.chipMon.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.MON, isChecked));
        binding.chipTue.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.TUE, isChecked));
        binding.chipWed.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.WED, isChecked));
        binding.chipThu.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.THU, isChecked));
        binding.chipFri.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.FRI, isChecked));
        binding.chipSat.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.SAT, isChecked));
        binding.chipSun.setOnCheckedChangeListener((buttonView, isChecked) -> workDaysMask = WorkDays.setEnabled(workDaysMask, WorkDays.SUN, isChecked));
    }

    private void applyDaysMaskToChips() {
        binding.chipMon.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.MON));
        binding.chipTue.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.TUE));
        binding.chipWed.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.WED));
        binding.chipThu.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.THU));
        binding.chipFri.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.FRI));
        binding.chipSat.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.SAT));
        binding.chipSun.setChecked(WorkDays.isEnabled(workDaysMask, WorkDays.SUN));
    }

    private void observeSelectedDayBookings() {
        if (bookingsLiveData != null) {
            bookingsLiveData.removeObservers(this);
        }
        bookingsLiveData = DbProvider.db(this).workBookingDao().observeForDay(cardId, selectedEpochDay);
        bookingsLiveData.observe(this, bookings -> {
            Map<Integer, WorkBookingEntity> byStart = new HashMap<>();
            if (bookings != null) {
                for (WorkBookingEntity b : bookings) {
                    byStart.put(b.startMinutes, b);
                }
            }

            List<Integer> starts = computeSlotStarts(sessionMinutes);
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
        String firstName = textOrNull(binding.inputFirstName.getText() != null ? binding.inputFirstName.getText().toString() : "");
        String lastName = textOrNull(binding.inputLastName.getText() != null ? binding.inputLastName.getText().toString() : "");
        String profession = textOrNull(binding.inputProfession.getText() != null ? binding.inputProfession.getText().toString() : "");

        Integer age = null;
        String ageStr = binding.inputAge.getText() != null ? binding.inputAge.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(ageStr)) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (Exception ignored) {}
        }

        int session = sessionMinutes;
        String sessionStr = binding.inputSessionMinutes.getText() != null ? binding.inputSessionMinutes.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(sessionStr)) {
            try {
                session = Math.max(15, Integer.parseInt(sessionStr));
            } catch (Exception ignored) {}
        }
        sessionMinutes = session;

        int finalSession = session;
        Integer finalAge = age;
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(this).cardDao().getByIdSync(cardId);
            if (card == null) return;
            card.firstName = firstName;
            card.lastName = lastName;
            card.profession = profession;
            card.age = finalAge;
            card.sessionMinutes = finalSession;
            card.workDaysMask = workDaysMask;
            DbProvider.db(this).cardDao().update(card);
        });

        observeSelectedDayBookings();
    }

    private void saveSlot(int startMinutes, String clientName) {
        String name = clientName != null ? clientName.trim() : "";
        DbProvider.io().execute(() -> {
            if (TextUtils.isEmpty(name)) {
                DbProvider.db(this).workBookingDao().deleteSlot(cardId, selectedEpochDay, startMinutes);
            } else {
                DbProvider.db(this).workBookingDao().insert(new WorkBookingEntity(cardId, selectedEpochDay, startMinutes, name));
            }
        });
    }

    private static String textOrNull(String value) {
        String v = value != null ? value.trim() : "";
        return TextUtils.isEmpty(v) ? null : v;
    }

    private static List<Integer> computeSlotStarts(int sessionMinutes) {
        int step = Math.max(15, sessionMinutes);
        List<Integer> starts = new ArrayList<>();
        for (int start = DAY_START_MIN; start + step <= DAY_END_MIN; start += step) {
            starts.add(start);
        }
        return starts;
    }
}