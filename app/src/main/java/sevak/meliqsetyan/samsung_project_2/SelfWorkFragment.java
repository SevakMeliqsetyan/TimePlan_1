package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.WorkBookingEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.WorkExperienceEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityWorkCardBinding;
import sevak.meliqsetyan.samsung_project_2.databinding.DialogAddExperienceBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private WorkExperienceAdapter experienceAdapter;
    private LiveData<List<WorkBookingEntity>> bookingsLiveData;
    private LiveData<List<WorkExperienceEntity>> experienceLiveData;

    // ПЕРЕМЕННЫЕ СЛОЖНОГО КАЛЕНДАРЯ УДАЛЕНЫ

    private boolean profileInitialized = false;
    private boolean chipsUpdating = false;
    private boolean forceShowProfile = false;

    private final ActivityResultLauncher<String> photoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
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
        Toast.makeText(getContext(), "Uploading photo...", Toast.LENGTH_SHORT).show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_photos")
                .child(myUid + "_" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    savePhotoUri(downloadUrl);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            binding.cardPhoto.setAlpha(1.0f);
                            loadPhoto(downloadUrl);
                            Toast.makeText(getContext(), "Photo updated", Toast.LENGTH_SHORT).show();
                        });
                    }
                }))
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            binding.cardPhoto.setAlpha(1.0f);
                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void loadPhoto(String url) {
        if (getContext() == null) return;
        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .centerCrop()
                .into(binding.cardPhoto);
    }

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

        experienceAdapter = new WorkExperienceAdapter(this::showDeleteExperienceConfirmation);
        binding.experienceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.experienceList.setAdapter(experienceAdapter);

        setupCalendar();
        setupDaysChips();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
        binding.btnAddExperience.setOnClickListener(v -> showAddExperienceDialog());
        binding.btnLogout.setOnClickListener(v -> logout());

        binding.cardPhotoContainer.setOnClickListener(v -> photoLauncher.launch("image/*"));

        binding.colorIndigo.setOnClickListener(v -> updateCardColor(0xFF818CF8));
        binding.colorTeal.setOnClickListener(v -> updateCardColor(0xFF2DD4BF));
        binding.colorRose.setOnClickListener(v -> updateCardColor(0xFFFB7185));
        binding.colorAmber.setOnClickListener(v -> updateCardColor(0xFFFBBF24));
        binding.colorSlate.setOnClickListener(v -> updateCardColor(0xFF94A3B8));

        binding.btnCreateWorkCard.setOnClickListener(v -> {
            forceShowProfile = true;
            binding.emptyWorkState.setVisibility(View.GONE);
            binding.profileCard.setVisibility(View.VISIBLE);
        });

        binding.btnViewRequests.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), RequestsActivity.class);
            intent.putExtra("CARD_ID", cardId);
            startActivity(intent);
        });

        binding.btnChangeLang.setOnClickListener(v -> showLanguageDialog());
        binding.btnChangeTheme.setOnClickListener(v -> showThemeDialog());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                binding.scheduleContainer.setVisibility(View.GONE);
                binding.myCardContainer.setVisibility(View.GONE);
                binding.profileCard.setVisibility(View.VISIBLE);
                return true;
            } else if (item.getItemId() == R.id.action_my_card) {
                binding.scheduleContainer.setVisibility(View.GONE);
                binding.profileCard.setVisibility(View.GONE);
                binding.myCardContainer.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        observeCard();
    }

    private void logout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout_btn, (dialog, which) -> {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAddExperienceDialog() {
        DialogAddExperienceBinding dBinding = DialogAddExperienceBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_experience)
                .setView(dBinding.getRoot())
                .setPositiveButton(R.string.dialog_add, (dialog, which) -> {
                    String label = dBinding.inputLabel.getText().toString().trim();
                    String value = dBinding.inputValue.getText().toString().trim();
                    if (!label.isEmpty() && !value.isEmpty()) {
                        DbProvider.io().execute(() -> {
                            DbProvider.db(requireContext()).workExperienceDao().insert(
                                    new WorkExperienceEntity(cardId, label, value)
                            );
                        });
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showDeleteExperienceConfirmation(WorkExperienceEntity experience) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_record_title)
                .setMessage(getString(R.string.delete_record_message, experience.label))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    DbProvider.io().execute(() -> {
                        DbProvider.db(requireContext()).workExperienceDao().deleteById(experience.id);
                    });
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void setupCalendar() {
        selectedEpochDay = TimeUtils.todayEpochDay();
        binding.selectedDayTitle.setText(TimeUtils.formatEpochDayLong(selectedEpochDay));

        // Настройка СТАНДАРТНОГО CalendarView
        java.util.Calendar cal = java.util.Calendar.getInstance();

        // Устанавливаем минимальную и максимальную дату
        binding.calendarView.setMinDate(cal.getTimeInMillis());
        cal.add(java.util.Calendar.MONTH, 6);
        binding.calendarView.setMaxDate(cal.getTimeInMillis());

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectCal = Calendar.getInstance();
            selectCal.set(year, month, dayOfMonth);
            selectedEpochDay = TimeUtils.toEpochDay(selectCal);
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

        String displayName = (card.firstName != null ? card.firstName : "") + " " + (card.lastName != null ? card.lastName : "");
        binding.toolbar.setTitle(displayName.trim());

        // Card Info
        binding.cardFullName.setText(displayName.trim());
        binding.cardProfession.setText(card.profession != null ? card.profession : "");
        binding.cardAge.setText(getString(R.string.work_age_label, (card.age != null ? String.valueOf(card.age) : "—")));

        if (card.backgroundColor != 0) {
            binding.myWorkCardView.setCardBackgroundColor(card.backgroundColor);
        }

        if (card.photoUri != null) {
            loadPhoto(card.photoUri);
        } else {
            binding.cardPhoto.setImageResource(R.drawable.ic_person);
        }

        boolean isProfileComplete = !TextUtils.isEmpty(card.firstName) && !TextUtils.isEmpty(card.lastName) && !TextUtils.isEmpty(card.profession);
        
        if (isProfileComplete) {
            binding.emptyWorkState.setVisibility(View.GONE);
            binding.scheduleContainer.setVisibility(View.VISIBLE);
            binding.profileCard.setVisibility(View.GONE);
            binding.myCardContainer.setVisibility(View.GONE);
            binding.btnViewRequests.setVisibility(View.VISIBLE);
            binding.btnDisconnect.setVisibility(View.GONE);
            binding.toolbar.getMenu().findItem(R.id.action_settings).setVisible(true);
            binding.toolbar.getMenu().findItem(R.id.action_my_card).setVisible(true);
        } else {
            if (forceShowProfile) {
                binding.emptyWorkState.setVisibility(View.GONE);
                binding.profileCard.setVisibility(View.VISIBLE);
            } else {
                binding.emptyWorkState.setVisibility(View.VISIBLE);
                binding.profileCard.setVisibility(View.GONE);
            }
            binding.scheduleContainer.setVisibility(View.GONE);
            binding.myCardContainer.setVisibility(View.GONE);
            binding.btnViewRequests.setVisibility(View.GONE);
            binding.toolbar.getMenu().findItem(R.id.action_settings).setVisible(false);
            binding.toolbar.getMenu().findItem(R.id.action_my_card).setVisible(false);
        }

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
        if (experienceLiveData != null) {
            experienceLiveData.removeObservers(getViewLifecycleOwner());
        }

        experienceLiveData = DbProvider.db(requireContext()).workExperienceDao().observeByCardId(cardId);
        experienceLiveData.observe(getViewLifecycleOwner(), exp -> experienceAdapter.submitList(exp));

        bookingsLiveData = DbProvider.db(requireContext()).workBookingDao().observeForDay(cardId, selectedEpochDay);
        bookingsLiveData.observe(getViewLifecycleOwner(), bookings -> {
            if (!isWorkDay(selectedEpochDay)) {
                binding.dayHint.setText(R.string.work_day_hint_off);
                slotsAdapter.submitList(new ArrayList<>());
                return;
            }
            binding.dayHint.setText(R.string.work_day_hint_working);

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
                ui.add(new WorkSlotsAdapter.WorkSlotUi(start, busy, false, busy ? b.clientName : null));
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

    private void savePhotoUri(String uri) {
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(requireContext()).cardDao().getSelfByTypeSync("WORK");
            if (card != null) {
                card.photoUri = uri;
                DbProvider.db(requireContext()).cardDao().update(card);
            }
        });
    }

    private void updateCardColor(int color) {
        DbProvider.io().execute(() -> {
            CardEntity card = DbProvider.db(requireContext()).cardDao().getSelfByTypeSync("WORK");
            if (card != null) {
                card.backgroundColor = color;
                DbProvider.db(requireContext()).cardDao().update(card);
            }
        });
    }

    private void showLanguageDialog() {
        String[] langs = {getString(R.string.lang_en), getString(R.string.lang_ru)};
        String[] codes = {"en", "ru"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings)
                .setItems(langs, (dialog, which) -> {
                    updateLocale(codes[which]);
                })
                .show();
    }

    private void updateLocale(String langCode) {
        ((MainApplication) requireActivity().getApplication()).setLanguage(langCode);
        requireActivity().recreate();
    }

    private void showThemeDialog() {
        String[] themes = {getString(R.string.theme_light), getString(R.string.theme_dark)};
        int[] modes = {androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_settings)
                .setItems(themes, (dialog, which) -> {
                    ((MainApplication) requireActivity().getApplication()).setThemeMode(modes[which]);
                })
                .show();
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
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(TimeUtils.epochDayToMillis(epochDay));
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 (Sun) to 7 (Sat)
        
        // Convert to 0 (Mon) to 6 (Sun) to match WorkDays masks
        int index;
        if (dayOfWeek == Calendar.SUNDAY) {
            index = 6;
        } else {
            index = dayOfWeek - 2;
        }
        
        int bit = 1 << index;
        return (workDaysMask & bit) != 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}