package com.example.banking;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Activity.AccountTransactionActivity;
import com.example.banking.Activity.BaseSecureActivity;
import com.example.banking.databinding.ActivityTopUpBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.model.SessionManager;
import com.example.banking.util.FirestoreHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class top_up extends BaseSecureActivity {

    private ActivityTopUpBinding binding;
    private FirebaseFirestore db;

    private List<AppCompatButton> amountButtons = new ArrayList<>();
    private double selectedAmount = 0;
    private Double currentBalance = 0.0;
    private String phone = "";

    private ListenerRegistration registration;

    private final String userId = SessionManager.getInstance().getUserId();
    private final String userPhone = SessionManager.getInstance().getPhone();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityTopUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.topUp, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        loadCheckingInfor();
        setupAmountButtons();
        setupPhoneInputListener();
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnContinue.setOnClickListener(v -> handleContinue());
        binding.btnPickContact.setOnClickListener(v -> pickContact());
    }

    // ======================= CONTINUE =======================
    private void handleContinue() {
        phone = binding.edtPhoneNumber.getText().toString().trim();

        if (phone.isEmpty()) {
            binding.edtPhoneNumber.setText(userPhone);
            return;
        }

        if (phone.length() < 9) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAmount <= 0) {
            Toast.makeText(this, "Vui lòng chọn mệnh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBalance < selectedAmount) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        createTopUpTransaction();
    }

    // ======================= CREATE TRANSACTION =======================
    private void createTopUpTransaction() {
        showLoading(true);
        String txId = db.collection("AccountTransactions").document().getId();

        AccountTransaction tx = new AccountTransaction();
        tx.setTransactionId(txId);
        tx.setUserId(userId);
        tx.setType("SERVICE");
        tx.setCategory("TOPUP");
        tx.setAmount(selectedAmount);
        tx.setStatus("PENDING");
        tx.setDescription("Nạp tiền điện thoại " + phone);
        tx.setReceiverName(phone);
        tx.setTimestamp(Timestamp.now());
        tx.setBiometricRequired(false);

        db.collection("AccountTransactions")
                .document(txId)
                .set(tx)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, AccountTransactionActivity.class);
                    intent.putExtra("TRANSACTION_ID", txId);
                    startActivity(intent);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(this, "Không tạo được giao dịch", Toast.LENGTH_SHORT).show();
                        }
                );
    }

    // ======================= AMOUNT =======================
    private void setupAmountButtons() {
        amountButtons.add(binding.btn10k);
        amountButtons.add(binding.btn20k);
        amountButtons.add(binding.btn50k);
        amountButtons.add(binding.btn100k);
        amountButtons.add(binding.btn200k);
        amountButtons.add(binding.btn500k);

        View.OnClickListener listener = v -> {
            for (AppCompatButton btn : amountButtons) btn.setSelected(false);
            AppCompatButton selectedBtn = (AppCompatButton) v;
            selectedBtn.setSelected(true);
            selectedAmount = Double.parseDouble(selectedBtn.getTag().toString());
        };

        for (AppCompatButton btn : amountButtons) {
            btn.setOnClickListener(listener);
        }
    }

    // ======================= PHONE =======================
    private void setupPhoneInputListener() {
        binding.edtPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phone = s.toString();
                String carrier = detectCarrier(phone);
                if (!carrier.isEmpty()) {
                    binding.tvCarrier.setVisibility(View.VISIBLE);
                    binding.tvCarrier.setText(carrier);
                } else {
                    binding.tvCarrier.setVisibility(View.GONE);
                }
            }
        });
    }

    private String detectCarrier(String phone) {
        if (phone.startsWith("03") || phone.startsWith("086") || phone.startsWith("096") ||
                phone.startsWith("097") || phone.startsWith("098")) return "Viettel";
        if (phone.startsWith("07") || phone.startsWith("089") ||
                phone.startsWith("090") || phone.startsWith("093")) return "MobiFone";
        if (phone.startsWith("08") || phone.startsWith("091") || phone.startsWith("094")) return "VinaPhone";
        if (phone.startsWith("05") || phone.startsWith("092")) return "Vietnamobile";
        return "";
    }

    // ======================= BALANCE =======================
    private void loadCheckingInfor() {
        FirestoreHelper helper = new FirestoreHelper();
        registration = helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance) {
                currentBalance = balance;
                binding.tvBalance.setText(String.format("%,.0f VND", balance));
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(top_up.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    // ======================= CONTACT =======================
    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Cursor cursor = getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                binding.edtPhoneNumber.setText(cursor.getString(0));
                cursor.close();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) registration.remove();
    }
}
