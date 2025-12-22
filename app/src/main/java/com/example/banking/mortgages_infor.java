package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Activity.AccountTransactionActivity;
import com.example.banking.Activity.BaseSecureActivity;
import com.example.banking.databinding.ActivityMortgagesInforBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.model.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class mortgages_infor extends BaseSecureActivity {

    private ActivityMortgagesInforBinding binding;
    private FirebaseFirestore db;

    private String accountNumber;
    private Double monthlyPayment = 0.0;

    private final String userId = SessionManager.getInstance().getUserId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMortgagesInforBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        accountNumber = getIntent().getStringExtra("account_number");

        if (accountNumber == null) {
            Toast.makeText(this, "Thiếu thông tin khoản vay", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadMortgageInfor();

        binding.btnPayMortgage.setOnClickListener(v -> createPendingTransaction());
    }

    /* ================= UI ================= */

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    /* ================= LOAD DATA ================= */

    private void loadMortgageInfor() {
        db.collection("Accounts")
                .whereEqualTo("account_number", accountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy khoản vay", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    var doc = query.getDocuments().get(0);

                    Double remainingDebt = doc.getDouble("remaining_debt");
                    Double interestRate = doc.getDouble("interest_rate");
                    monthlyPayment = doc.getDouble("monthly_payment");
                    Date periodDay = doc.getDate("next_payment_date");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    binding.tvMortgagePrincipal.setText(
                            remainingDebt != null
                                    ? String.format("%,.0f VND", remainingDebt)
                                    : "—"
                    );

                    binding.tvMortgageRate.setText(
                            interestRate != null
                                    ? interestRate + "% / năm"
                                    : "—"
                    );

                    binding.tvMonthlyPayment.setText(
                            monthlyPayment != null
                                    ? String.format("%,.0f VND", monthlyPayment)
                                    : "0 VND"
                    );

                    if (periodDay != null) {
                        binding.tvNextDueDate.setText(sdf.format(periodDay));
                    } else {
                        binding.tvNextDueDate.setText("Chưa có lịch trả");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thông tin khoản vay", Toast.LENGTH_SHORT).show()
                );
    }

    /* ================= TRANSACTION ================= */

    private void createPendingTransaction() {

        if (monthlyPayment == null || monthlyPayment <= 0) {
            Toast.makeText(this, "Số tiền thanh toán không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        String month = new SimpleDateFormat("MM/yyyy", Locale.getDefault())
                .format(new Date());

        DocumentReference txRef =
                db.collection("AccountTransactions").document();

        AccountTransaction tx = new AccountTransaction();
        tx.setTransactionId(txRef.getId());
        tx.setUserId(userId);

        // ===== TYPE & CATEGORY =====
        tx.setType("PAY_MORTGAGE");
        tx.setCategory("MORTGAGE");

        // ===== AMOUNT =====
        tx.setAmount(monthlyPayment);

        // ===== STATUS =====
        tx.setStatus("PENDING");

        // ===== RECEIVER (KHOẢN VAY) =====
        tx.setReceiverAccountNumber(accountNumber);
        tx.setReceiverName("Khoản vay cá nhân");
        tx.setReceiverBankName("West Bank");

        // ===== DESCRIPTION =====
        tx.setDescription("Thanh toán khoản vay kỳ " + month);

        // ===== BALANCE =====
        tx.setBalanceBefore(null);   // chưa trừ
        tx.setBalanceAfter(null);    // chưa có

        // ===== TIME =====
        tx.setTimestamp(Timestamp.now());

        txRef.set(tx)
                .addOnSuccessListener(v -> {
                            showLoading(false);
                    openTransactionActivity(txRef.getId());
                        }
                )
                .addOnFailureListener(e -> {
                            showLoading(false);
                    Toast.makeText(
                            this,
                            "Không thể tạo giao dịch",
                            Toast.LENGTH_SHORT
                    ).show();
                        }
                );
    }

    private void openTransactionActivity(String transactionId) {
        Intent intent = new Intent(this, AccountTransactionActivity.class);
        intent.putExtra("TRANSACTION_ID", transactionId);
        startActivity(intent);
    }
}
