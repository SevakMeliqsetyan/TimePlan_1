package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

// Импорт для ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CardsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Добавлено super

        adapter = new CardsAdapter(card -> {
            if ("PERSONAL".equals(card.type)) {
                Intent intent = new Intent(requireContext(), PersonalCardActivity.class);
                intent.putExtra(PersonalCardActivity.EXTRA_CARD_ID, card.id);
                startActivity(intent);
            } else {
                Intent intent = new Intent(requireContext(), WorkCardActivity.class);
                intent.putExtra(WorkCardActivity.EXTRA_CARD_ID, card.id);
                startActivity(intent);
            }
        });

        binding.cardsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.cardsList.setAdapter(adapter);

        // Наблюдаем за списком карточек
        DbProvider.db(requireContext()).cardDao().observeAll().observe(getViewLifecycleOwner(), cards -> {
            adapter.submitList(cards);
            boolean empty = cards == null || cards.isEmpty();
            binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.cardsList.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        // ИСПРАВЛЕНО: Используем тип ExtendedFloatingActionButton вместо FloatingActionButton
        ExtendedFloatingActionButton fab = binding.fabAddCard;
        fab.setOnClickListener(v -> AddCardBottomSheet.show(requireActivity().getSupportFragmentManager()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}