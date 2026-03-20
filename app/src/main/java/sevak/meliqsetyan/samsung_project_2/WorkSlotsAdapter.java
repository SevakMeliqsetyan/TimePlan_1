package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import sevak.meliqsetyan.samsung_project_2.databinding.ItemWorkSlotBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class WorkSlotsAdapter extends ListAdapter<WorkSlotsAdapter.WorkSlotUi, WorkSlotsAdapter.VH> {

    public static class WorkSlotUi {
        public final int startMinutes;
        public final boolean busy;
        public final String clientName;

        public WorkSlotUi(int startMinutes, boolean busy, String clientName) {
            this.startMinutes = startMinutes;
            this.busy = busy;
            this.clientName = clientName;
        }
    }

    public interface Listener {
        void onSlotClick(WorkSlotUi slot);
    }

    private final Listener listener;

    public WorkSlotsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorkSlotBinding binding = ItemWorkSlotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemWorkSlotBinding binding;

        VH(ItemWorkSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(WorkSlotUi slot, Listener listener) {
            // Исправлено: используем slotTime вместо time
            binding.slotTime.setText(TimeUtils.formatTimeMinutes(slot.startMinutes));

            if (slot.busy) {
                // Исправлено: используем slotTitle вместо client
                binding.slotTitle.setText(slot.clientName);
                binding.slotTitle.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_primary));
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.slot_busy_background));
                binding.root.setStrokeColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.slot_border));
            } else {
                binding.slotTitle.setText(binding.getRoot().getContext().getString(R.string.work_slot_free));
                binding.slotTitle.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.surface_elevated_dark));
                binding.root.setStrokeColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.divider_dark));
            }

            binding.getRoot().setOnClickListener(v -> listener.onSlotClick(slot));
        }
    }

    private static final DiffUtil.ItemCallback<WorkSlotUi> DIFF = new DiffUtil.ItemCallback<WorkSlotUi>() {
        @Override
        public boolean areItemsTheSame(@NonNull WorkSlotUi oldItem, @NonNull WorkSlotUi newItem) {
            return oldItem.startMinutes == newItem.startMinutes;
        }

        @Override
        public boolean areContentsTheSame(@NonNull WorkSlotUi oldItem, @NonNull WorkSlotUi newItem) {
            return oldItem.startMinutes == newItem.startMinutes
                    && oldItem.busy == newItem.busy
                    && ((oldItem.clientName == null && newItem.clientName == null) ||
                    (oldItem.clientName != null && oldItem.clientName.equals(newItem.clientName)));
        }
    };
}