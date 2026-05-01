package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import sevak.meliqsetyan.samsung_project_2.data.db.WorkExperienceEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ItemExperienceBinding;

public class WorkExperienceAdapter extends ListAdapter<WorkExperienceEntity, WorkExperienceAdapter.ViewHolder> {

    public interface Listener {
        void onDelete(WorkExperienceEntity experience);
    }

    private final Listener listener;

    public WorkExperienceAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<WorkExperienceEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull WorkExperienceEntity oldItem, @NonNull WorkExperienceEntity newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull WorkExperienceEntity oldItem, @NonNull WorkExperienceEntity newItem) {
                return oldItem.label.equals(newItem.label) && oldItem.value.equals(newItem.value);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExperienceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkExperienceEntity item = getItem(position);
        holder.binding.textLabel.setText(item.label);
        holder.binding.textValue.setText(item.value);
        holder.binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExperienceBinding binding;
        ViewHolder(ItemExperienceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}