package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import sevak.meliqsetyan.samsung_project_2.data.WorkDays;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.databinding.BottomsheetAddCardBinding;

import androidx.core.content.ContextCompat;


public class AddCardBottomSheet extends BottomSheetDialogFragment {

    private BottomsheetAddCardBinding binding;
    private String selectedType = "PERSONAL";

    public static void show(@NonNull androidx.fragment.app.FragmentManager fm) {
        new AddCardBottomSheet().show(fm, "add_card");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomsheetAddCardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        updateSelectionUi();

        binding.btnPersonal.setOnClickListener(v -> {
            selectedType = "PERSONAL";
            updateSelectionUi();
        });
        binding.btnWork.setOnClickListener(v -> {
            selectedType = "WORK";
            updateSelectionUi();
        });

        binding.btnCreate.setOnClickListener(v -> {
            String title = binding.titleInput.getText() != null ? binding.titleInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                binding.titleInputLayout.setError("Введите название");
                return;
            }
            binding.titleInputLayout.setError(null);

            CardEntity entity = new CardEntity(selectedType, title, System.currentTimeMillis());
            if ("WORK".equals(selectedType)) {
                entity.sessionMinutes = 90;
                entity.workDaysMask = WorkDays.defaultMonToFri();
            }

            DbProvider.io().execute(() -> DbProvider.db(requireContext()).cardDao().insert(entity));
            dismiss();
        });
    }

    private void updateSelectionUi() {
        if (binding == null) return;
        boolean personal = "PERSONAL".equals(selectedType);

        // Устанавливаем толщину обводки
        binding.btnPersonal.setStrokeWidth(personal ? 4 : 2);
        binding.btnWork.setStrokeWidth(!personal ? 4 : 2);

        // Исправляем установку цвета обводки:
        binding.btnPersonal.setStrokeColor(
                ContextCompat.getColorStateList(requireContext(), personal ? R.color.teal_accent : R.color.divider_dark)
        );

        binding.btnWork.setStrokeColor(
                ContextCompat.getColorStateList(requireContext(), !personal ? R.color.purple_accent : R.color.divider_dark)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

