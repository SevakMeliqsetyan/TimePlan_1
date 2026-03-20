package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ItemCardBinding;

public class CardsAdapter extends ListAdapter<CardEntity, CardsAdapter.VH> {

    public interface Listener {
        void onCardClick(CardEntity card);
    }

    private final Listener listener;

    public CardsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCardBinding binding = ItemCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CardEntity card = getItem(position);
        holder.bind(card, listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemCardBinding binding;

        VH(ItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CardEntity card, Listener listener) {
            binding.title.setText(card.title);

            if ("PERSONAL".equals(card.type)) {
                binding.typePill.setText(binding.getRoot().getContext().getString(R.string.home_add_personal));
                binding.typePill.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.teal_accent));
                binding.subtitle.setText("Календарь + дела на день");
            } else {
                binding.typePill.setText(binding.getRoot().getContext().getString(R.string.home_add_work));
                binding.typePill.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.purple_accent));
                binding.subtitle.setText("Расписание клиентов + календарь");
            }

            binding.getRoot().setOnClickListener(v -> listener.onCardClick(card));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onCardClick(card);
                return true;
            });
        }
    }

    private static final DiffUtil.ItemCallback<CardEntity> DIFF = new DiffUtil.ItemCallback<CardEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull CardEntity oldItem, @NonNull CardEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CardEntity oldItem, @NonNull CardEntity newItem) {
            return oldItem.createdAtEpochMs == newItem.createdAtEpochMs
                    && oldItem.type.equals(newItem.type)
                    && oldItem.title.equals(newItem.title);
        }
    };
}

