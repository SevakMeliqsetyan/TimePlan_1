package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import sevak.meliqsetyan.samsung_project_2.data.db.PersonalTaskEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ItemPersonalTaskBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class PersonalTasksAdapter extends ListAdapter<PersonalTaskEntity, PersonalTasksAdapter.VH> {

    public interface Listener {
        void onDeleteClick(PersonalTaskEntity task);
    }

    private final Listener listener;

    public PersonalTasksAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonalTaskBinding binding = ItemPersonalTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemPersonalTaskBinding binding;

        VH(ItemPersonalTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PersonalTaskEntity task, Listener listener) {
            binding.time.setText(TimeUtils.formatTimeMinutes(task.timeMinutes));
            binding.title.setText(task.title);
            
            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(task);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<PersonalTaskEntity> DIFF = new DiffUtil.ItemCallback<PersonalTaskEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull PersonalTaskEntity oldItem, @NonNull PersonalTaskEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PersonalTaskEntity oldItem, @NonNull PersonalTaskEntity newItem) {
            return oldItem.cardId == newItem.cardId
                    && oldItem.dateEpochDay == newItem.dateEpochDay
                    && oldItem.timeMinutes == newItem.timeMinutes
                    && oldItem.title.equals(newItem.title);
        }
    };
}