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

import com.example.officer.R;
import com.example.officer.databinding.ActivityOpenMortgageBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class open_mortgage extends BaseSecureActivity {

    private ActivityOpenMortgageBinding binding;

    private FirebaseFirestore db;
    private Double rate = 0.0;
    private Double monthlyPayment = 0.0;

    private String customer_ID, customer_email;

    private ActivityResultLauncher<Intent> otpLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityOpenMortgageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.openMortgage, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });

        // Init loading overlay
        initLoading(binding.getRoot());

        customer_ID = getIntent().getStringExtra("customer_ID");
        customer_email = getIntent().getStringExtra("email");

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> finish());

        initOtpLauncher();
        loadInterestRate();
        setupPurposeSpinner();
        setupTextWatchers();

        binding.btnSubmit.setOnClickListener(v -> submit());
    }

    // ================= OTP =================

    private void initOtpLauncher() {
        otpLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if ("OK".equalsIgnoreCase(
                                result.getData().getStringExtra("result_key"))) {
                            openMortgage(customer_ID);
                        }
                    }
                }
        );
    }

    // ================= UI =================

    private void setupPurposeSpinner() {
        String[] purposes = {"Mua nhà", "Mua xe", "Kinh doanh", "Tiêu dùng cá nhân"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                purposes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPurpose.setAdapter(adapter);
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void afterTextChanged(Editable s) { calMonthlyPayment(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        };

        binding.edtLoanAmount.addTextChangedListener(watcher);
        binding.edtYears.addTextChangedListener(watcher);
    }

    // ================= SUBMIT =================

    private void submit() {
        if (binding.edtLoanAmount.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.edtYears.getText().toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập thời gian vay", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, otp.class);
        intent.putExtra("email", customer_email);
        otpLauncher.launch(intent);
    }

    // ================= CALC =================

    private void calMonthlyPayment() {
        try {
            double loanAmount = Double.parseDouble(
                    binding.edtLoanAmount.getText().toString());
            int years = Integer.parseInt(
                    binding.edtYears.getText().toString());

            int totalMonths = years * 12;
            double monthlyRate = (rate / 100) / 12;

            monthlyPayment = (loanAmount * monthlyRate)
                    / (1 - Math.pow(1 + monthlyRate, -totalMonths));

            binding.tvMonthlyPayment.setText(
                    String.format("%,.0f VND", monthlyPayment));
        } catch (Exception e) {
            binding.tvMonthlyPayment.setText("0 VND");
        }
    }

    private void loadInterestRate() {
        db.collection("InterestRates")
                .whereEqualTo("interest_type", "mortgage")
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        rate = snap.getDocuments()
                                .get(0)
                                .getDouble("interest_rate");
                        binding.tvInterestRate.setText(rate + "%/năm");
                    }
                });
    }

    // ================= CREATE MORTGAGE =================

    private void openMortgage(String userId) {

        showLoading(true);

        double amount = Double.parseDouble(binding.edtLoanAmount.getText().toString());
        int years = Integer.parseInt(binding.edtYears.getText().toString());
        int totalMonths = years * 12;

        String mortgageAccountNumber = generateAccountNumber("03");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);

        /* ========== 1. QUERY CHECKING ACCOUNT (NGOÀI TRANSACTION) ========== */
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(this,
                                "Không tìm thấy tài khoản thanh toán",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentReference checkingRef =
                            querySnapshot.getDocuments().get(0).getReference();

                    /* ========== 2. TRANSACTION ========== */
                    db.runTransaction(transaction -> {

                        DocumentSnapshot checkingDoc =
                                transaction.get(checkingRef);

                        Double checkingBalance =
                                checkingDoc.getDouble("balance");
                        if (checkingBalance == null) {
                            throw new RuntimeException(
                                    "Số dư tài khoản không hợp lệ");
                        }

                        /* ========== 3. TẠO MORTGAGE ACCOUNT ========== */
                        Map<String, Object> mortgage = new HashMap<>();
                        mortgage.put("account_number", mortgageAccountNumber);
                        mortgage.put("user_id", userId);
                        mortgage.put("account_type", "MORTGAGE");
                        mortgage.put("purpose",
                                binding.spinnerPurpose.getSelectedItem().toString());

                        mortgage.put("loan_amount", amount);
                        mortgage.put("remaining_debt", amount);
                        mortgage.put("monthly_payment", monthlyPayment);

                        mortgage.put("interest_rate", rate);
                        mortgage.put("total_months", totalMonths);
                        mortgage.put("paid_months", 0);

                        mortgage.put("created_at",
                                FieldValue.serverTimestamp());
                        mortgage.put("next_payment_date",
                                new Timestamp(cal.getTime()));
                        mortgage.put("status", "ACTIVE");

                        DocumentReference mortgageRef =
                                db.collection("Accounts")
                                        .document(mortgageAccountNumber);
                        transaction.set(mortgageRef, mortgage);

                        /* ========== 4. CỘNG TIỀN VÀO CHECKING ========== */
                        double newCheckingBalance =
                                checkingBalance + amount;
                        transaction.update(checkingRef,
                                "balance", newCheckingBalance);

                        /* ========== 5. LƯU ACCOUNT TRANSACTION ========== */
                        String txId = db.collection("AccountTransactions")
                                .document().getId();

                        Map<String, Object> tx = new HashMap<>();
                        tx.put("transactionId", txId);
                        tx.put("userId", userId);
                        tx.put("type", "MORTGAGE_DISBURSE");
                        tx.put("amount", amount);
                        tx.put("status", "SUCCESS");
                        tx.put("timestamp", Timestamp.now());

                        tx.put("senderAccountNumber", "BANK");
                        tx.put("receiverAccountNumber",
                                checkingDoc.getString("account_number"));

                        tx.put("description",
                                "Giải ngân khoản vay thế chấp");
                        tx.put("category", "MORTGAGE");
                        tx.put("balanceAfter",
                                newCheckingBalance);

                        DocumentReference txRef =
                                db.collection("AccountTransactions")
                                        .document(txId);
                        transaction.set(txRef, tx);

                        return null;

                    }).addOnSuccessListener(v -> {
                        showLoading(false);
                        Toast.makeText(this,
                                "Tạo khoản vay & giải ngân thành công",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this,
                                "Lỗi tạo khoản vay: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });

                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Lỗi truy vấn tài khoản thanh toán",
                            Toast.LENGTH_LONG).show();
                });
    }


    private String generateAccountNumber(String type) {
        String branchCode = "1010";
        String random = String.format("%06d",
                new Random().nextInt(1_000_000));
        return branchCode + type + random;
    }
}
