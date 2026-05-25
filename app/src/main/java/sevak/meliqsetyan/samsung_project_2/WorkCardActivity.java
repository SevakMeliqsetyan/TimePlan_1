package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.BookingRequestEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.UserProfileEntity;
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
    private LiveData<List<BookingRequestEntity>> requestsLiveData;

    private boolean profileInitialized = false;

    private boolean isSelfCard = false;
    private WorkExperienceAdapter experienceAdapter;
    private long lastClickTime = 0;

    private final androidx.activity.result.ActivityResultLauncher<String> photoLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadPhotoToFirebase(uri);
                }
            }
    );

    private void uploadPhotoToFirebase(android.net.Uri fileUri) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        binding.cardPhoto.setAlpha(0.5f);
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_photos")
                .child(myUid + "_" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    savePhotoUri(downloadUrl);
                    runOnUiThread(() -> {
                        binding.cardPhoto.setAlpha(1.0f);
                        loadPhoto(downloadUrl);
                        Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show();
                    });
                }))
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        binding.cardPhoto.setAlpha(1.0f);
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void loadPhoto(String url) {
        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .centerCrop()
                .into(binding.cardPhoto);
    }

    private void savePhotoUri(String uri) {
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(this).cardDao().getByIdSync(cardId);
            if (card != null) {
                card.photoUri = uri;
                DbProvider.db(this).cardDao().update(card);
            }
        });
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

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
                Toast.makeText(this, R.string.past_day_edit_error, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isSelfCard) {
                EditWorkSlotDialog.show(
                        getSupportFragmentManager(),
                        TimeUtils.formatTimeMinutes(slot.startMinutes),
                        slot.busy ? slot.clientName : "",
                        clientName -> saveSlot(slot.startMinutes, clientName)
                );
            } else {
                if (slot.busy) {
                    Toast.makeText(this, R.string.slot_occupied, Toast.LENGTH_SHORT).show();
                } else {
                    showBookingRequestDialog(slot);
                }
            }
        });
        binding.slotsList.setLayoutManager(new LinearLayoutManager(this));
        binding.slotsList.setAdapter(slotsAdapter);

        experienceAdapter = new WorkExperienceAdapter(null);
        binding.experienceList.setLayoutManager(new LinearLayoutManager(this));
        binding.experienceList.setAdapter(experienceAdapter);

        setupCalendar();
        setupDaysChips();
        setupColorPicker();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        binding.btnDisconnect.setOnClickListener(v -> confirmDisconnect());
        binding.btnViewRequests.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, RequestsActivity.class);
            intent.putExtra("CARD_ID", cardId);
            startActivity(intent);
        });

        DbProvider.db(this).cardDao().observeById(cardId).observe(this, card -> {
            if (card == null) return;
            binding.toolbar.setTitle(card.title);
            isSelfCard = card.isSelf;

            binding.scheduleContainer.setVisibility(View.VISIBLE);

            if (isSelfCard) {
                binding.btnViewRequests.setVisibility(View.VISIBLE);
                binding.profileCard.setVisibility(View.VISIBLE);
                binding.myCardContainer.setVisibility(View.GONE);
                binding.btnSaveProfile.setVisibility(View.VISIBLE);
                binding.btnDisconnect.setVisibility(View.GONE);
                binding.cardPhotoContainer.setOnClickListener(v -> {
                    if (System.currentTimeMillis() - lastClickTime < 500) return;
                    lastClickTime = System.currentTimeMillis();
                    try {
                        photoLauncher.launch("image/*");
                    } catch (Exception e) {
                        Toast.makeText(this, "Error opening gallery", Toast.LENGTH_SHORT).show();
                    }
                });
                enableEditing(true);
            } else {
                binding.btnViewRequests.setVisibility(View.GONE);
                binding.profileCard.setVisibility(View.GONE);
                binding.myCardContainer.setVisibility(View.VISIBLE);
                binding.btnSaveProfile.setVisibility(View.GONE);
                binding.btnDisconnect.setVisibility(View.VISIBLE);

                // Заполняем данные для просмотра
                String fullName = (card.firstName != null ? card.firstName : "") + " " + (card.lastName != null ? card.lastName : "");
                binding.cardFullName.setText(fullName.trim().isEmpty() ? card.title : fullName.trim());
                binding.cardProfession.setText(card.profession != null ? card.profession : "");
                binding.cardAge.setText(getString(R.string.work_age_label, (card.age != null ? String.valueOf(card.age) : "—")));
                binding.cardCity.setText(card.city != null ? getString(R.string.city_label, card.city) : "");
                binding.btnAddExperience.setVisibility(View.GONE);
                binding.colorPickerContainer.setVisibility(View.GONE);
                binding.labelTheme.setVisibility(View.GONE);
            }

            if (card.backgroundColor != 0) {
                binding.myWorkCardView.setCardBackgroundColor(card.backgroundColor);
            }

            if (card.photoUri != null) {
                loadPhoto(card.photoUri);
            } else {
                binding.cardPhoto.setImageResource(R.drawable.ic_person);
            }

            if (!profileInitialized) {
                profileInitialized = true;
                binding.inputFirstName.setText(card.firstName != null ? card.firstName : "");
                binding.inputLastName.setText(card.lastName != null ? card.lastName : "");
                binding.inputAge.setText(card.age != null ? String.valueOf(card.age) : "");
                binding.inputProfession.setText(card.profession != null ? card.profession : "");
                binding.inputCity.setText(card.city != null ? card.city : "");
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

            DbProvider.db(this).workExperienceDao().observeByCardId(cardId).observe(this, exp -> {
                if (experienceAdapter != null) experienceAdapter.submitList(exp);
            });
        });
    }

    private void setupColorPicker() {
        binding.colorIndigo.setOnClickListener(v -> updateCardColor(0xFF818CF8));
        binding.colorTeal.setOnClickListener(v -> updateCardColor(0xFF2DD4BF));
        binding.colorRose.setOnClickListener(v -> updateCardColor(0xFFFB7185));
        binding.colorAmber.setOnClickListener(v -> updateCardColor(0xFFFBBF24));
        binding.colorSlate.setOnClickListener(v -> updateCardColor(0xFF94A3B8));
        binding.colorViolet.setOnClickListener(v -> updateCardColor(0xFF8B5CF6));
        binding.colorEmerald.setOnClickListener(v -> updateCardColor(0xFF10B981));
        binding.colorOrange.setOnClickListener(v -> updateCardColor(0xFFF59E0B));
    }

    private void updateCardColor(int color) {
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(this).cardDao().getByIdSync(cardId);
            if (card != null) {
                card.backgroundColor = color;
                DbProvider.db(this).cardDao().update(card);
            }
        });
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Ограничиваем выбор в календаре (визуально)
        binding.calendarView.setMinDate(System.currentTimeMillis() - 1000);

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            selectedEpochDay = TimeUtils.toEpochDay(cal);
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
        if (requestsLiveData != null) {
            requestsLiveData.removeObservers(this);
        }

        bookingsLiveData = DbProvider.db(this).workBookingDao().observeForDay(cardId, selectedEpochDay);
        requestsLiveData = DbProvider.db(this).bookingRequestDao().observePendingForCard(cardId);

        // Используем MediatorLiveData или просто обновляем при изменении любого из источников
        bookingsLiveData.observe(this, bookings -> updateSlotsUi());
        requestsLiveData.observe(this, requests -> updateSlotsUi());
    }

    private void updateSlotsUi() {
        List<WorkBookingEntity> bookings = bookingsLiveData.getValue();
        List<BookingRequestEntity> requests = requestsLiveData.getValue();

        Map<Integer, WorkBookingEntity> bookingsMap = new HashMap<>();
        if (bookings != null) {
            for (WorkBookingEntity b : bookings) {
                bookingsMap.put(b.startMinutes, b);
            }
        }

        Map<Integer, BookingRequestEntity> requestsMap = new HashMap<>();
        if (requests != null) {
            for (BookingRequestEntity r : requests) {
                if (r.dateEpochDay == selectedEpochDay) {
                    requestsMap.put(r.startMinutes, r);
                }
            }
        }

        List<Integer> starts = computeSlotStarts(sessionMinutes);
        List<WorkSlotsAdapter.WorkSlotUi> ui = new ArrayList<>(starts.size());
        for (int start : starts) {
            WorkBookingEntity b = bookingsMap.get(start);
            BookingRequestEntity r = requestsMap.get(start);

            boolean busy = b != null && !TextUtils.isEmpty(b.clientName);
            boolean pending = r != null;

            ui.add(new WorkSlotsAdapter.WorkSlotUi(start, busy, pending, busy ? b.clientName : null));
        }
        slotsAdapter.submitList(ui);
    }

    private void enableEditing(boolean enabled) {
        binding.inputFirstName.setEnabled(enabled);
        binding.inputLastName.setEnabled(enabled);
        binding.inputAge.setEnabled(enabled);
        binding.inputProfession.setEnabled(enabled);
        binding.inputSessionMinutes.setEnabled(enabled);
        binding.chipMon.setClickable(enabled);
        binding.chipTue.setClickable(enabled);
        binding.chipWed.setClickable(enabled);
        binding.chipThu.setClickable(enabled);
        binding.chipFri.setClickable(enabled);
        binding.chipSat.setClickable(enabled);
        binding.chipSun.setClickable(enabled);
    }

    private void showBookingRequestDialog(WorkSlotsAdapter.WorkSlotUi slot) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.booking_request_dialog_title)
                .setMessage(getString(R.string.booking_request_dialog_message, 
                        TimeUtils.formatEpochDayLong(selectedEpochDay), 
                        TimeUtils.formatTimeMinutes(slot.startMinutes)))
                .setPositiveButton(R.string.btn_send_request, (dialog, which) -> sendBookingRequest(slot))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void sendBookingRequest(WorkSlotsAdapter.WorkSlotUi slot) {
        DbProvider.io().execute(() -> {
            CardEntity targetCard = DbProvider.db(this).cardDao().getByIdSync(cardId);
            if (targetCard == null || targetCard.ownerUid == null) {
                runOnUiThread(() -> Toast.makeText(this, "Cannot send request: target user ID unknown", Toast.LENGTH_SHORT).show());
                return;
            }

            CardEntity myWorkCard = DbProvider.db(this).cardDao().getSelfWorkCard();
            String myName = "Unknown User";
            String myUid = FirebaseAuth.getInstance().getUid();

            if (myWorkCard != null) {
                String fullName = (myWorkCard.firstName != null ? myWorkCard.firstName : "") + " " + (myWorkCard.lastName != null ? myWorkCard.lastName : "");
                myName = fullName.trim().isEmpty() ? myWorkCard.title : fullName.trim();
            } else {
                UserProfileEntity profile = DbProvider.db(this).userProfileDao().getFirstSync();
                if (profile != null) {
                    myName = (profile.firstName + " " + profile.lastName).trim();
                }
            }

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("targetOwnerUid", targetCard.ownerUid);
            requestData.put("requesterUid", myUid);
            requestData.put("requesterName", myName);
            requestData.put("dateEpochDay", selectedEpochDay);
            requestData.put("startMinutes", slot.startMinutes);
            requestData.put("status", "PENDING");
            requestData.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("booking_requests")
                    .add(requestData)
                    .addOnSuccessListener(documentReference -> {
                        runOnUiThread(() -> Toast.makeText(this, R.string.request_sent_success, Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    });
        });
    }

    private void saveProfile() {
        String firstName = textOrNull(binding.inputFirstName.getText() != null ? binding.inputFirstName.getText().toString() : "");
        String lastName = textOrNull(binding.inputLastName.getText() != null ? binding.inputLastName.getText().toString() : "");
        String profession = textOrNull(binding.inputProfession.getText() != null ? binding.inputProfession.getText().toString() : "");
        String city = textOrNull(binding.inputCity.getText() != null ? binding.inputCity.getText().toString() : "");
        String myUid = FirebaseAuth.getInstance().getUid();

        if (city != null && !city.matches("[a-zA-Z\\s\\-]+")) {
            binding.inputCityLayout.setError(getString(R.string.error_english_only));
            return;
        }
        binding.inputCityLayout.setError(null);

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
            card.city = city;
            card.age = finalAge;
            card.sessionMinutes = finalSession;
            card.workDaysMask = workDaysMask;
            if (card.isSelf) {
                card.ownerUid = myUid;
                // Также обновляем основной профиль пользователя
                UserProfileEntity profile = DbProvider.db(this).userProfileDao().getFirstSync();
                if (profile != null) {
                    profile.firstName = firstName;
                    profile.lastName = lastName;
                    profile.city = city;
                    DbProvider.db(this).userProfileDao().update(profile);
                }
            }
            DbProvider.db(this).cardDao().update(card);
        });

        observeSelectedDayBookings();
    }

    private void confirmDisconnect() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.disconnect_confirm_title)
                .setMessage(R.string.disconnect_confirm_message)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> disconnectCard())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void disconnectCard() {
        DbProvider.io().execute(() -> {
            DbProvider.db(this).cardDao().deleteById(cardId);
            runOnUiThread(() -> {
                Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
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
