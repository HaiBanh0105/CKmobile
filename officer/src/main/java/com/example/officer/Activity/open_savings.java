package com.example.officer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.officer.databinding.OpenSavingsBinding;
import com.example.officer.util.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class open_savings extends BaseSecureActivity {

    private OpenSavingsBinding binding;
    private FirebaseFirestore db;

    private double rate = 0;
    private double profit = 0;
    private int months = 6;
    private Date maturityDate;

    private ActivityResultLauncher<Intent> otpLauncher;
    private double pendingAmount;

    private String userId;
    private String customerEmail;

    // ================= LIFECYCLE =================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = OpenSavingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.openSaving, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Nhận userId + email từ màn trước
        Intent intent = getIntent();
        userId = intent.getStringExtra("customer_ID");
        customerEmail = intent.getStringExtra("email");

        if (userId == null)
            finish();
        if (customerEmail == null)
            finish();

        setupToolbar();
        setupTermDropdown();
        setupListeners();
        initOtpLauncher();

        loadCheckingInfo();
        loadInterestRate();
    }

    // ================= UI =================

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTermDropdown() {
        String[] terms = {"3 Tháng", "6 Tháng", "12 Tháng", "24 Tháng", "Không thời hạn"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                terms
        );
        binding.autoCompleteTerm.setAdapter(adapter);
        binding.autoCompleteTerm.setText("6 Tháng", false);
        updateMaturityDate();
    }

    private void setupListeners() {

        binding.edtAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                calculateProfit();
            }
        });

        binding.autoCompleteTerm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                updateMaturityDate();
                calculateProfit();
            }
        });

        binding.btnConfirmOpen.setOnClickListener(v -> openOtpActivity());
    }

    // ================= LOAD DATA =================

    private void loadCheckingInfo() {
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        Double balance = snapshot.getDocuments().get(0).getDouble("balance");
                        binding.tvSourceBalance.setText(
                                String.format("%,.0f VND", balance)
                        );
                    }
                });
    }

    private void loadInterestRate() {
        db.collection("InterestRates")
                .whereEqualTo("interest_type", "savings")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        rate = snapshot.getDocuments().get(0).getDouble("interest_rate");
                        binding.tvAppliedRate.setText(rate + "% / năm");
                    }
                });
    }

    // ================= CALCULATION =================

    private void updateMaturityDate() {
        String term = binding.autoCompleteTerm.getText().toString();

        months = 0;
        if (term.contains("3")) months = 3;
        else if (term.contains("6")) months = 6;
        else if (term.contains("12")) months = 12;
        else if (term.contains("24")) months = 24;

        if (months == 0) {
            binding.tvMaturityDate.setText("Không thời hạn");
            maturityDate = null;
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MONTH, months);

        maturityDate = cal.getTime();

        binding.tvMaturityDate.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(maturityDate)
        );
    }

    private void calculateProfit() {
        String raw = binding.edtAmount.getText().toString().replaceAll("[^\\d]", "");
        if (raw.isEmpty() || rate == 0) {
            binding.tvEstimatedProfit.setText("0 VND");
            return;
        }

        double amount = Double.parseDouble(raw);
        profit = (amount * rate / 100) / 12 * months;

        binding.tvEstimatedProfit.setText(
                String.format("%,.0f VND", profit)
        );
    }

    // ================= OTP =================

    private void initOtpLauncher() {
        otpLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String res = result.getData().getStringExtra("result_key");
                        if ("OK".equals(res)) {
                            openSavingAtomic(pendingAmount);
                        }
                    }
                }
        );
    }

    private void openOtpActivity() {

        String raw = binding.edtAmount.getText().toString().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(raw);
        if (amount <= 0) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingAmount = amount;

        Intent intent = new Intent(this, otp.class);
        intent.putExtra("email", customerEmail);

        otpLauncher.launch(intent);
    }

    // ================= BUSINESS =================

    private String generateAccountNumber(String prefix) {
        return prefix + UUID.randomUUID()
                .toString()
                .replaceAll("[^\\d]", "")
                .substring(0, 8);
    }

    private void openSavingAtomic(double amount) {

        String savingsAccountNumber = generateAccountNumber("02");
        showLoading(true);

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(this, "Không tìm thấy tài khoản nguồn", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot checkingDoc = snapshot.getDocuments().get(0);

                    db.runTransaction((Transaction.Function<Void>) transaction -> {

                                DocumentSnapshot freshChecking =
                                        transaction.get(checkingDoc.getReference());

                                double currentBalance =
                                        freshChecking.getDouble("balance") != null
                                                ? freshChecking.getDouble("balance")
                                                : 0;

                                if (currentBalance < amount)
                                    throw new RuntimeException("INSUFFICIENT");

                                double newBalance = currentBalance - amount;

                                transaction.update(
                                        checkingDoc.getReference(),
                                        "balance",
                                        newBalance
                                );

                                Map<String, Object> saving = new HashMap<>();
                                saving.put("user_id", userId);
                                saving.put("account_number", savingsAccountNumber);
                                saving.put("account_type", "savings");
                                saving.put("balance", amount);
                                saving.put("interest_rate", rate);
                                saving.put("period_months", months);
                                saving.put("status", "active");
                                saving.put("created_at", FieldValue.serverTimestamp());
                                if (maturityDate != null)
                                    saving.put("maturity_date", maturityDate);

                                transaction.set(
                                        db.collection("Accounts").document(),
                                        saving
                                );

                                String txnId =
                                        db.collection("AccountTransactions").document().getId();

                                Map<String, Object> txn = new HashMap<>();
                                txn.put("transactionId", txnId);
                                txn.put("userId", userId);
                                txn.put("type", "OPEN_SAVINGS");
                                txn.put("amount", amount);
                                txn.put("balanceAfter", newBalance);
                                txn.put("status", "SUCCESS");
                                txn.put("timestamp", FieldValue.serverTimestamp());
                                txn.put("senderAccountNumber",
                                        freshChecking.getString("account_number"));
                                txn.put("receiverAccountNumber",
                                        savingsAccountNumber);
                                txn.put("description",
                                        "Mở sổ tiết kiệm " +
                                                (months == 0 ? "không thời hạn" : months + " tháng"));

                                transaction.set(
                                        db.collection("AccountTransactions")
                                                .document(txnId),
                                        txn
                                );

                                return null;
                            })
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(
                                        this,
                                        "Mở tài khoản tiết kiệm thành công",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(
                                        this,
                                        e.getMessage().contains("INSUFFICIENT")
                                                ? "Số dư không đủ"
                                                : "Lỗi hệ thống",
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                });
    }
}
