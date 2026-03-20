package sevak.meliqsetyan.samsung_project_2;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

import sevak.meliqsetyan.samsung_project_2.databinding.DialogAddPersonalTaskBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class AddPersonalTaskDialog extends DialogFragment {

    public interface Listener {
        void onCreate(int timeMinutes, String title);
    }

    private static Listener staticListener;

    public static void show(@NonNull androidx.fragment.app.FragmentManager fm, @NonNull Listener listener) {
        staticListener = listener;
        new AddPersonalTaskDialog().show(fm, "add_personal_task");
    }

    private DialogAddPersonalTaskBinding binding;
    private int selectedTimeMinutes;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddPersonalTaskBinding.inflate(requireActivity().getLayoutInflater());

        Calendar now = Calendar.getInstance();
        selectedTimeMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        updateTimeText();

        binding.btnTime.setOnClickListener(v -> {
            int h = selectedTimeMinutes / 60;
            int m = selectedTimeMinutes % 60;
            new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                selectedTimeMinutes = hourOfDay * 60 + minute;
                updateTimeText();
            }, h, m, true).show();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setPositiveButton(R.string.personal_add_task_button, (dialog, which) -> {
                    String title = binding.titleInput.getText() != null ? binding.titleInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) return;
                    if (staticListener != null) {
                        staticListener.onCreate(selectedTimeMinutes, title);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void updateTimeText() {
        if (binding == null) return;
        binding.btnTime.setText(TimeUtils.formatTimeMinutes(selectedTimeMinutes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

