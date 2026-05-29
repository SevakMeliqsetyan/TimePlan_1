package sevak.meliqsetyan.samsung_project_2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sevak.meliqsetyan.samsung_project_2.data.db.CardEntity;
import sevak.meliqsetyan.samsung_project_2.data.db.DbProvider;
import sevak.meliqsetyan.samsung_project_2.databinding.ActivityExploreBinding;

public class ExploreActivity extends AppCompatActivity {
    private ActivityExploreBinding binding;
    private CardsAdapter adapter;
    private List<CardEntity> allCards = new ArrayList<>();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("language", "en");
        super.attachBaseContext(MainApplication.updateLocale(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExploreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new CardsAdapter(this::showAddConfirmation);
        binding.exploreList.setLayoutManager(new LinearLayoutManager(this));
        binding.exploreList.setAdapter(adapter);

        setupSearch();
        loadAllUsersCards();
    }

    private void setupSearch() {
        TextWatcher searchWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCards();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        binding.etSearchCity.addTextChangedListener(searchWatcher);
        binding.etSearchProfession.addTextChangedListener(searchWatcher);
    }

    private void filterCards() {
        String cityQuery = binding.etSearchCity.getText().toString().toLowerCase().trim();
        String professionQuery = binding.etSearchProfession.getText().toString().toLowerCase().trim();

        List<CardEntity> filtered = new ArrayList<>();
        for (CardEntity card : allCards) {
            boolean matchesCity = true;
            if (!cityQuery.isEmpty()) {
                matchesCity = card.city != null && card.city.toLowerCase().contains(cityQuery);
            }

            boolean matchesProfession = true;
            if (!professionQuery.isEmpty()) {
                matchesProfession = card.profession != null && card.profession.toLowerCase().contains(professionQuery);
            }

            if (matchesCity && matchesProfession) {
                filtered.add(card);
            }
        }

        adapter.submitList(filtered);

        if (filtered.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.exploreList.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.exploreList.setVisibility(View.VISIBLE);
        }
    }

    private void loadAllUsersCards() {
        String myUid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("users_public_cards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allCards.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CardEntity card = doc.toObject(CardEntity.class);
                        if (card != null && !card.ownerUid.equals(myUid)) {
                            allCards.add(card);
                        }
                    }
                    filterCards();
                })
                .addOnFailureListener(e -> {
                    Log.e("Explore", "Error loading cards", e);
                    Toast.makeText(this, "Error loading cards", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddConfirmation(CardEntity card) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_card_confirm_title)
                .setMessage(getString(R.string.add_card_confirm_message, card.title))
                .setPositiveButton(R.string.btn_create, (dialog, which) -> addCardToConnections(card))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void addCardToConnections(CardEntity scannedCard) {
        DbProvider.io().execute(() -> {
            try {
                var db = DbProvider.db(this);
                CardEntity existing = null;
                if (scannedCard.ownerUid != null) {
                    existing = db.cardDao().getByOwnerUidSync(scannedCard.ownerUid);
                }

                if (existing != null) {
                    scannedCard.id = existing.id;
                    scannedCard.isSelf = false;
                    db.cardDao().update(scannedCard);
                    db.workExperienceDao().deleteByCardId(existing.id);
                } else {
                    scannedCard.id = 0;
                    scannedCard.isSelf = false;
                    scannedCard.id = db.cardDao().insert(scannedCard);
                }

                if (scannedCard.experienceList != null && scannedCard.id > 0) {
                    for (var exp : scannedCard.experienceList) {
                        exp.id = 0;
                        exp.cardId = scannedCard.id;
                        db.workExperienceDao().insert(exp);
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.qr_scan_success, Toast.LENGTH_SHORT).show();
                    sendMyCardToTarget(scannedCard.ownerUid);
                });
            } catch (Exception e) {
                Log.e("Explore", "Error saving card", e);
            }
        });
    }

    private void sendMyCardToTarget(String targetUid) {
        if (targetUid == null) return;
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        DbProvider.io().execute(() -> {
            var db = DbProvider.db(this);
            CardEntity myCard = db.cardDao().getSelfWorkCard();
            if (myCard == null) {
                List<CardEntity> selfCards = db.cardDao().getSelfCardsSync();
                if (!selfCards.isEmpty()) myCard = selfCards.get(0);
            }

            if (myCard == null) return;

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
            cardToSend.experienceList = db.workExperienceDao().getByCardIdSync(myCard.id);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(targetUid)
                    .collection("incoming_cards")
                    .document(myUid)
                    .set(cardToSend);
        });
    }
}