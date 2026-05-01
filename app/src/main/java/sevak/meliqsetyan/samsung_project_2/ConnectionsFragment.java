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

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import sevak.meliqsetyan.samsung_project_2.data.db.AppDatabase;
import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.databinding.FragmentConnectionsBinding;

public class ConnectionsFragment extends Fragment {
    private FragmentConnectionsBinding binding;
    private CardsAdapter adapter;
    private AppDatabase db;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
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
        loadConnections();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.connectionsList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CardsAdapter(new ArrayList<>(), card -> {
            // Handle card click (optional: open full view)
            Intent intent = new Intent(getContext(), WorkCardActivity.class);
            intent.putExtra("CARD_ID", card.id);
            startActivity(intent);
        });
        binding.connectionsList.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnMyQr.setOnClickListener(v -> showMyQrDialog());
        binding.btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.btn_scan_qr));
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });
    }

    private void showMyQrDialog() {
        new Thread(() -> {
            // Get the first self work card to share
            CardEntity selfCard = db.cardDao().getSelfWorkCard();
            if (selfCard == null) {
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "Create a work card first", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            try {
                // Remove ID to not conflict on another device
                selfCard.id = 0;
                selfCard.isSelf = false;
                
                String json = new Gson().toJson(selfCard);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(json, BarcodeFormat.QR_CODE, 600, 600);

                getActivity().runOnUiThread(() -> {
                    ImageView imageView = new ImageView(getContext());
                    imageView.setPadding(40, 40, 40, 40);
                    imageView.setImageBitmap(bitmap);

                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.qr_dialog_title)
                            .setView(imageView)
                            .setPositiveButton("OK", null)
                            .show();
                });
            } catch (Exception e) {
                Log.e("QR", "Error generating QR", e);
            }
        }).start();
    }

    private void processScannedData(String data) {
        try {
            CardEntity scannedCard = new Gson().fromJson(data, CardEntity.class);
            scannedCard.isSelf = false; // It's a connection, not me
            scannedCard.createdAtEpochMs = System.currentTimeMillis();

            new Thread(() -> {
                db.cardDao().insert(scannedCard);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), R.string.qr_scan_success, Toast.LENGTH_SHORT).show();
                    loadConnections();
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.qr_scan_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadConnections() {
        new Thread(() -> {
            List<CardEntity> connections = db.cardDao().getOtherCards();
            getActivity().runOnUiThread(() -> {
                if (connections.isEmpty()) {
                    binding.connectionsList.setVisibility(View.GONE);
                } else {
                    binding.connectionsList.setVisibility(View.VISIBLE);
                    adapter.updateCards(connections);
                }
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}