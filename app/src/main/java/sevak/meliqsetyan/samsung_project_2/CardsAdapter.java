package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

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

    public CardsAdapter(java.util.List<CardEntity> initialList, Listener listener) {
        super(DIFF);
        this.listener = listener;
        submitList(initialList);
    }

    public void updateCards(java.util.List<CardEntity> newList) {
        submitList(newList);
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
            String displayName = card.title;
            if (card.firstName != null || card.lastName != null) {
                displayName = ((card.firstName != null ? card.firstName : "") + " " + (card.lastName != null ? card.lastName : "")).trim();
                if (displayName.isEmpty()) displayName = card.title;
            }
            binding.title.setText(displayName);

            if ("PERSONAL".equals(card.type)) {
                binding.typePill.setText(binding.getRoot().getContext().getString(R.string.home_add_personal));
                binding.typePill.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.teal_accent));
                binding.subtitle.setText(R.string.card_type_personal_desc);
            } else {
                binding.typePill.setText(binding.getRoot().getContext().getString(R.string.home_add_work));
                binding.typePill.setBackgroundTintList(ContextCompat.getColorStateList(binding.getRoot().getContext(), R.color.purple_accent));
                binding.subtitle.setText(card.profession != null ? card.profession : binding.getRoot().getContext().getString(R.string.card_type_work_desc));
            }

            if (card.backgroundColor != 0) {
                binding.cardView.setCardBackgroundColor(card.backgroundColor);
            } else {
                binding.cardView.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.white));
            }

            if (card.photoUri != null) {
                Glide.with(binding.getRoot().getContext())
                        .load(card.photoUri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .centerCrop()
                        .into(binding.photo);
            } else {
                binding.photo.setImageResource(R.drawable.ic_person);
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
            if (oldItem.type == null || newItem.type == null) return false;
            if (oldItem.title == null || newItem.title == null) return false;

            return oldItem.createdAtEpochMs == newItem.createdAtEpochMs
                    && oldItem.type.equals(newItem.type)
                    && oldItem.title.equals(newItem.title);
        }
    };
}

