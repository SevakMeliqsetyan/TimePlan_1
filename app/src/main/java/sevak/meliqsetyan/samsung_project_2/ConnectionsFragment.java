package sevak.meliqsetyan.samsung_project_2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.AppDatabase;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.databinding.FragmentConnectionsBinding;

public class ConnectionsFragment extends Fragment {
    private FragmentConnectionsBinding binding;
    private CardsAdapter adapter;
    private AppDatabase db;
    private long lastClickTime = 0;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(getContext(), R.string.scan_cancelled, Toast.LENGTH_SHORT).show();
                } else {
                    processScannedData(result.getContents());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentConnectionsBinding.inflate(inflater, container, false);
        db = DbProvider.db(requireContext());

        setupRecyclerView();
        setupButtons();
        observeConnections();

        return binding.getRoot();
    }

    private void observeConnections() {
        db.cardDao().observeOtherCards().observe(getViewLifecycleOwner(), connections -> {
            if (connections == null || connections.isEmpty()) {
                binding.connectionsList.setVisibility(View.GONE);
            } else {
                binding.connectionsList.setVisibility(View.VISIBLE);
                adapter.submitList(connections);
            }
        });
    }

    private void setupRecyclerView() {
        binding.connectionsList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CardsAdapter(card -> {
            if ("PERSONAL".equals(card.type)) {
                Intent intent = new Intent(getContext(), PersonalCardActivity.class);
                intent.putExtra(PersonalCardActivity.EXTRA_CARD_ID, card.id);
                startActivity(intent);
            } else {
                Intent intent = new Intent(getContext(), WorkCardActivity.class);
                intent.putExtra(WorkCardActivity.EXTRA_CARD_ID, card.id);
                startActivity(intent);
            }
        });
        binding.connectionsList.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnMyQr.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < 500) return;
            lastClickTime = System.currentTimeMillis();
            showMyQrDialog();
        });
        binding.btnScanQr.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < 500) return;
            lastClickTime = System.currentTimeMillis();

            if (!isAdded() || getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.INITIALIZED)) {
                try {
                    ScanOptions options = new ScanOptions();
                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                    options.setPrompt(getString(R.string.scan_prompt));
                    options.setCameraId(0);
                    options.setBeepEnabled(true);
                    options.setBarcodeImageEnabled(true);
                    options.setOrientationLocked(false);
                    barcodeLauncher.launch(options);
                } catch (Exception e) {
                    Log.e("ConnectionsFragment", "Failed to launch barcode scanner", e);
                    Toast.makeText(getContext(), "Scanner error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showMyQrDialog() {
        DbProvider.io().execute(() -> {
            CardEntity selfCard = db.cardDao().getSelfWorkCard();
            String myUid = FirebaseAuth.getInstance().getUid();
            
            if (selfCard == null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Сначала создайте рабочую карту", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            if (myUid == null) return;

            try {
                CardEntity qrCard = new CardEntity(selfCard.type, selfCard.title, selfCard.createdAtEpochMs, false);
                qrCard.firstName = selfCard.firstName;
                qrCard.lastName = selfCard.lastName;
                qrCard.profession = selfCard.profession;
                qrCard.age = selfCard.age;
                qrCard.sessionMinutes = selfCard.sessionMinutes;
                qrCard.workDaysMask = selfCard.workDaysMask;
                qrCard.workStartMinutes = selfCard.workStartMinutes;
                qrCard.workEndMinutes = selfCard.workEndMinutes;
                qrCard.breakStartMinutes = selfCard.breakStartMinutes;
                qrCard.breakEndMinutes = selfCard.breakEndMinutes;
                qrCard.photoUri = selfCard.photoUri;
                qrCard.backgroundColor = selfCard.backgroundColor;
                qrCard.ownerUid = myUid;
                
                // Fetch experience list for QR
                qrCard.experienceList = db.workExperienceDao().getByCardIdSync(selfCard.id);
                
                String json = new Gson().toJson(qrCard);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(json, BarcodeFormat.QR_CODE, 600, 600);

                getActivity().runOnUiThread(() -> {
                    ImageView imageView = new ImageView(getContext());
                    imageView.setPadding(40, 40, 40, 40);
                    imageView.setImageBitmap(bitmap);

                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.my_qr_code_title)
                            .setView(imageView)
                            .setPositiveButton(R.string.dialog_cancel, null)
                            .show();
                });
            } catch (Exception e) {
                Log.e("QR", "Error generating QR", e);
            }
        });
    }

    private void processScannedData(String data) {
        try {
            CardEntity scannedCard = new Gson().fromJson(data, CardEntity.class);
            if (scannedCard != null && scannedCard.type != null && scannedCard.title != null) {
                scannedCard.id = 0; // Ensure it's treated as a new entry
                scannedCard.isSelf = false;
                
                DbProvider.io().execute(() -> {
                    try {
                        // Check if already exists by ownerUid
                        CardEntity existing = null;
                        if (scannedCard.ownerUid != null) {
                            existing = db.cardDao().getByOwnerUidSync(scannedCard.ownerUid);
                        }
                        
                        if (existing != null) {
                            scannedCard.id = existing.id;
                            db.cardDao().update(scannedCard);
                            // Clean old experience to avoid duplicates
                            db.workExperienceDao().deleteByCardId(existing.id);
                        } else {
                            // Use a transaction or ensure the card is inserted before experience
                            scannedCard.id = db.cardDao().insert(scannedCard);
                        }
                        
                        // Save shared experience records
                        if (scannedCard.experienceList != null && scannedCard.id > 0) {
                            for (sevak.meliqsetyan.samsung_project_2.data.db.WorkExperienceEntity exp : scannedCard.experienceList) {
                                exp.id = 0; // New ID for local DB
                                exp.cardId = scannedCard.id;
                                db.workExperienceDao().insert(exp);
                            }
                        }
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), R.string.qr_scan_success, Toast.LENGTH_SHORT).show()
                            );
                        }
                        
                        // Optional: send my card back if targetUid is present
                        if (scannedCard.ownerUid != null) {
                            sendMyCardToTarget(scannedCard.ownerUid);
                        }
                    } catch (Exception e) {
                        Log.e("QR", "Error saving scanned card", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(getContext(), R.string.qr_scan_error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), R.string.qr_scan_error, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("QR", "Error parsing QR data", e);
            Toast.makeText(getContext(), R.string.qr_scan_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void addRandomizedTestCard() {
        DbProvider.io().execute(() -> {
            // Count existing cards to make a unique name
            int count = db.cardDao().getOtherCardsSync().size() + 1;
            
            String firstName = "Sevak " + count;
            String lastName = "Meliqsetyan";
            String profession = "Android Developer";
            int age = 19;
            
            CardEntity testCard = new CardEntity("WORK", firstName + " " + lastName, System.currentTimeMillis(), false);
            testCard.firstName = firstName;
            testCard.lastName = lastName;
            testCard.profession = profession;
            testCard.age = age;
            testCard.sessionMinutes = 45;
            testCard.workDaysMask = 62; // Mon-Fri
            
            // Cycle through some colors
            int[] colors = {0xFF818CF8, 0xFF2DD4BF, 0xFFFB7185, 0xFFFBBF24, 0xFF8B5CF6, 0xFF10B981};
            testCard.backgroundColor = colors[count % colors.length];
            
            testCard.ownerUid = "test_user_" + System.currentTimeMillis();
            
            db.cardDao().insert(testCard);
            
            getActivity().runOnUiThread(() -> 
                Toast.makeText(getContext(), "Test card added: " + firstName, Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void sendMyCardToTarget(String targetUid) {
        if (targetUid == null) return;
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        DbProvider.io().execute(() -> {
            CardEntity myCard = db.cardDao().getSelfWorkCard();
            if (myCard == null) {
                List<CardEntity> selfCards = db.cardDao().getSelfCardsSync();
                if (!selfCards.isEmpty()) myCard = selfCards.get(0);
            }

            if (myCard == null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Создайте свою карту для авто-ответа", Toast.LENGTH_LONG).show()
                );
                return;
            }

            CardEntity cardToSend = new CardEntity(myCard.type, myCard.title, myCard.createdAtEpochMs, false);
            cardToSend.firstName = myCard.firstName;
            cardToSend.lastName = myCard.lastName;
            cardToSend.profession = myCard.profession;
            cardToSend.age = myCard.age;
            cardToSend.sessionMinutes = myCard.sessionMinutes;
            cardToSend.workDaysMask = myCard.workDaysMask;
            cardToSend.workStartMinutes = myCard.workStartMinutes;
            cardToSend.workEndMinutes = myCard.workEndMinutes;
            cardToSend.breakStartMinutes = myCard.breakStartMinutes;
            cardToSend.breakEndMinutes = myCard.breakEndMinutes;
            cardToSend.photoUri = myCard.photoUri;
            cardToSend.backgroundColor = myCard.backgroundColor;
            cardToSend.ownerUid = myUid;
            
            // Fetch experience list for sharing
            cardToSend.experienceList = db.workExperienceDao().getByCardIdSync(myCard.id);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(targetUid)
                    .collection("incoming_cards")
                    .document(myUid)
                    .set(cardToSend)
                    .addOnSuccessListener(aVoid -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), R.string.card_sent_success, Toast.LENGTH_SHORT).show()
                            );
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error sending card", e));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
