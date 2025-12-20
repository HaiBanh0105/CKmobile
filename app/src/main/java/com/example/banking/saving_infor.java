package com.example.banking;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Activity.BaseSecureActivity;
import com.example.banking.Fragment.OtpDialogFragment;
import com.example.banking.databinding.SavingsInforBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class saving_infor extends BaseSecureActivity {

    private SavingsInforBinding binding;

    private String savingsAccountNumber;
    private String savingsDocId;
    private String checkingDocId;
    private String userId;

    private double currentBalance = 0;
    private double estProfit = 0;

    private double interestRate = 0;   // lãi suất cam kết
    private double appliedRate = 0;    // lãi suất áp dụng khi rút

    private Date maturityDate;
    private Date createdAt;            // ngày mở sổ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = SavingsInforBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.savingInfor, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userId = SessionManager.getInstance().getUserId();
        savingsAccountNumber = getIntent().getStringExtra("account_number");

        if (savingsAccountNumber == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCheckingAccount();
        loadSavingInfor();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnWithdraw.setOnClickListener(v -> handleWithdraw());
    }

    /* ================= LOAD DATA ================= */

    private void loadCheckingAccount() {
        FirebaseFirestore.getInstance()
                .collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        checkingDocId = snapshot.getDocuments().get(0).getId();
                    }
                });
    }

    private void loadSavingInfor() {
        FirebaseFirestore.getInstance()
                .collection("Accounts")
                .whereEqualTo("account_number", savingsAccountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    var doc = snapshot.getDocuments().get(0);
                    savingsDocId = doc.getId();

                    currentBalance = doc.getDouble("balance") != null
                            ? doc.getDouble("balance") : 0;

                    createdAt = doc.getDate("created_at");
                    maturityDate = doc.getDate("maturity_date");

                    Long months = doc.getLong("period_months");
                    interestRate = doc.getDouble("interest_rate") != null
                            ? doc.getDouble("interest_rate") : 0;

                    binding.tvPeriod.setText(
                            months == null || months == 0
                                    ? "Không thời hạn"
                                    : months + " tháng"
                    );

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    binding.tvMaturityDate.setText(
                            maturityDate != null ? sdf.format(maturityDate) : "Không thời hạn"
                    );

                    binding.tvSavingsRate.setText(interestRate + "% / năm");

                    if (months != null && months > 0) {
                        estProfit = (currentBalance * interestRate / 100) / 12 * months;
                        binding.tvSavingsProfit.setText(
                                String.format("+ %,.0f VND", estProfit)
                        );
                    }
                });
    }

    /* ================= WITHDRAW ================= */

    private void handleWithdraw() {
        Date today = Calendar.getInstance().getTime();

        if (maturityDate != null && today.before(maturityDate)) {
            new AlertDialog.Builder(this)
                    .setTitle("Tất toán trước hạn")
                    .setMessage(
                            "Sổ chưa đến ngày đáo hạn (" +
                                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .format(maturityDate) +
                                    ").\n\nBạn sẽ chỉ hưởng lãi suất không kỳ hạn (0.1%)."
                    )
                    .setPositiveButton("Tiếp tục rút",
                            (dialog, which) -> showOtpDialog())
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            showOtpDialog();
        }
    }

    private void showOtpDialog() {
        if (checkingDocId == null) {
            Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
            return;
        }

        new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                calculateFinalInterestRate();
                performWithdrawalAtomic();
            }

            @Override
            public void onOtpFailed() {
                Toast.makeText(saving_infor.this,
                        "Xác thực OTP thất bại",
                        Toast.LENGTH_SHORT).show();
            }
        }).show(getSupportFragmentManager(), "OTP_DIALOG");
    }

    /* ================= INTEREST ================= */

    private void calculateFinalInterestRate() {
        Date today = Calendar.getInstance().getTime();

        if (maturityDate != null && today.before(maturityDate)) {
            appliedRate = 0.1; // rút trước hạn
        } else {
            appliedRate = interestRate; // đúng / sau hạn
        }
    }

    /* ================= TRANSACTION ================= */

    private void performWithdrawalAtomic() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        showLoading(true);
        DocumentReference checkingRef =
                db.collection("Accounts").document(checkingDocId);
        DocumentReference savingsRef =
                db.collection("Accounts").document(savingsDocId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {

            Double checkingBal =
                    transaction.get(checkingRef).getDouble("balance");

            if (checkingBal == null) checkingBal = 0.0;

            Date withdrawDate = Calendar.getInstance().getTime();
            long days =
                    TimeUnit.DAYS.convert(
                            withdrawDate.getTime() - createdAt.getTime(),
                            TimeUnit.MILLISECONDS
                    );

            double actualProfit =
                    (currentBalance * appliedRate * days) / 365 / 100;

            double totalAmount = currentBalance + actualProfit;

            transaction.update(checkingRef,
                    "balance", checkingBal + totalAmount);

            transaction.delete(savingsRef);

            String txnId =
                    db.collection("AccountTransactions").document().getId();

            Map<String, Object> txn = new HashMap<>();
            txn.put("transactionId", txnId);
            txn.put("userId", userId);
            txn.put("type", "WITHDRAW_SAVINGS");
            txn.put("amount", totalAmount);
            txn.put("status", "SUCCESS");
            txn.put("timestamp", FieldValue.serverTimestamp());
            txn.put("description",
                    "Tất toán tiết kiệm " + savingsAccountNumber);

            transaction.set(
                    db.collection("AccountTransactions").document(txnId),
                    txn
            );

            return null;
        }).addOnSuccessListener(v -> {
            showLoading(false);
            Toast.makeText(this,
                    "Rút tiền thành công",
                    Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this,
                    "Lỗi: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }
}
