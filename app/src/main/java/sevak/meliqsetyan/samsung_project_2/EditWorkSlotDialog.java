package sevak.meliqsetyan.samsung_project_2;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import sevak.meliqsetyan.samsung_project_2.databinding.DialogEditWorkSlotBinding;

public class EditWorkSlotDialog extends DialogFragment {

    public interface Listener {
        void onSave(String clientName);
    }

    private static Listener staticListener;
    private static String staticTime;
    private static String staticInitialName;

    public static void show(@NonNull androidx.fragment.app.FragmentManager fm,
                            @NonNull String timeText,
                            @Nullable String initialName,
                            @NonNull Listener listener) {
        staticListener = listener;
        staticTime = timeText;
        staticInitialName = initialName;
        new EditWorkSlotDialog().show(fm, "edit_work_slot");
    }

    private DialogEditWorkSlotBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogEditWorkSlotBinding.inflate(requireActivity().getLayoutInflater());
        binding.title.setText("Слот " + staticTime);
        if (!TextUtils.isEmpty(staticInitialName)) {
            binding.inputClient.setText(staticInitialName);
            binding.inputClient.setSelection(staticInitialName.length());
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot())
                .setPositiveButton(R.string.work_slot_save, (dialog, which) -> {
                    String name = binding.inputClient.getText() != null ? binding.inputClient.getText().toString().trim() : "";
                    if (staticListener != null) {
                        staticListener.onSave(name);
                    }
                })
                .setNeutralButton("Очистить", (dialog, which) -> {
                    if (staticListener != null) {
                        staticListener.onSave("");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

