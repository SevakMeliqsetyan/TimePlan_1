package sevak.meliqsetyan.samsung_project_2;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import sevak.meliqsetyan.samsung_project_2.data.db.BookingRequestEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ItemBookingRequestBinding;
import sevak.meliqsetyan.samsung_project_2.util.TimeUtils;

public class BookingRequestsAdapter extends ListAdapter<BookingRequestEntity, BookingRequestsAdapter.VH> {

    public interface Listener {
        void onAccept(BookingRequestEntity request);
        void onDecline(BookingRequestEntity request);
    }

    private final Listener listener;

    public BookingRequestsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBookingRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        private final ItemBookingRequestBinding binding;

        VH(ItemBookingRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BookingRequestEntity request, Listener listener) {
            binding.requesterName.setText(request.requesterName);
            String details = TimeUtils.formatEpochDayLong(request.dateEpochDay) + ", " + TimeUtils.formatTimeMinutes(request.startMinutes);
            binding.requestDetails.setText(details);

            binding.btnAccept.setOnClickListener(v -> listener.onAccept(request));
            binding.btnDecline.setOnClickListener(v -> listener.onDecline(request));
        }
    }

    private static final DiffUtil.ItemCallback<BookingRequestEntity> DIFF = new DiffUtil.ItemCallback<BookingRequestEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull BookingRequestEntity oldItem, @NonNull BookingRequestEntity newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull BookingRequestEntity oldItem, @NonNull BookingRequestEntity newItem) {
            return oldItem.isPending == newItem.isPending && oldItem.requesterName.equals(newItem.requesterName);
        }
    };
}
