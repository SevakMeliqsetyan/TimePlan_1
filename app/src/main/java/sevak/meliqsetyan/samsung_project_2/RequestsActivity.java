package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.BookingRequestEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.data.db.WorkBookingEntity;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityRequestsBinding;

public class RequestsActivity extends AppCompatActivity {
    private ActivityRequestsBinding binding;
    private long cardId;
    private ListenerRegistration firestoreListener;
    private BookingRequestsAdapter adapter;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cardId = getIntent().getLongExtra("CARD_ID", -1);
        if (cardId == -1) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.requestsList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingRequestsAdapter(new BookingRequestsAdapter.Listener() {
            @Override
            public void onAccept(BookingRequestEntity request) {
                acceptRequest(request);
            }

            @Override
            public void onDecline(BookingRequestEntity request) {
                declineRequest(request);
            }
        });
        binding.requestsList.setAdapter(adapter);

        setupFirestoreListener();
    }

    private void setupFirestoreListener() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        firestoreListener = FirebaseFirestore.getInstance().collection("booking_requests")
                .whereEqualTo("targetOwnerUid", myUid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<BookingRequestEntity> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            String requesterName = doc.getString("requesterName");
                            Long dateEpochDay = doc.getLong("dateEpochDay");
                            Long startMinutes = doc.getLong("startMinutes");
                            
                            BookingRequestEntity entity = new BookingRequestEntity(
                                    cardId, 0, requesterName, 
                                    dateEpochDay != null ? dateEpochDay : 0, 
                                    startMinutes != null ? startMinutes.intValue() : 0
                            );
                            // We use the ID to track the Firestore document ID indirectly
                            // Room ID isn't needed here.
                            requests.add(entity);
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    if (requests.isEmpty()) {
                        binding.requestsList.setVisibility(View.GONE);
                        binding.emptyView.setVisibility(View.VISIBLE);
                    } else {
                        binding.requestsList.setVisibility(View.VISIBLE);
                        binding.emptyView.setVisibility(View.GONE);
                        adapter.submitList(requests);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) firestoreListener.remove();
    }

    private void acceptRequest(BookingRequestEntity request) {
        DbProvider.io().execute(() -> {
            WorkBookingEntity booking = new WorkBookingEntity(
                    request.cardId,
                    request.dateEpochDay,
                    request.startMinutes,
                    request.requesterName
            );
            DbProvider.db(this).workBookingDao().insert(booking);
            
            updateFirestoreRequestStatus(request, "ACCEPTED");

            runOnUiThread(() -> Toast.makeText(this, R.string.request_accepted, Toast.LENGTH_SHORT).show());
        });
    }

    private void declineRequest(BookingRequestEntity request) {
        DbProvider.io().execute(() -> {
            updateFirestoreRequestStatus(request, "DECLINED");
            runOnUiThread(() -> Toast.makeText(this, R.string.request_declined, Toast.LENGTH_SHORT).show());
        });
    }

    private void updateFirestoreRequestStatus(BookingRequestEntity request, String status) {
        FirebaseFirestore.getInstance().collection("booking_requests")
                .whereEqualTo("targetOwnerUid", FirebaseAuth.getInstance().getUid())
                .whereEqualTo("dateEpochDay", request.dateEpochDay)
                .whereEqualTo("startMinutes", request.startMinutes)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("status", status);
                    }
                });
    }
}
