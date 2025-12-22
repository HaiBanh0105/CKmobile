package com.example.banking;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Activity.BaseSecureActivity;
import com.example.banking.Fragment.OtpDialogFragment;
import com.example.banking.databinding.OpenSavingsBinding;
import com.example.banking.model.SessionManager;
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

    private final String userId = SessionManager.getInstance().getUserId();

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

        setupToolbar();
        setupTermDropdown();
        loadCheckingInfo();
        loadInterestRate();
        setupListeners();
    }

    // ================= UI SETUP =================

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
            @Override
            public void afterTextChanged(Editable s) {
                calculateProfit();
            }
        });

        binding.autoCompleteTerm.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateMaturityDate();
                calculateProfit();
            }
        });

        binding.btnConfirmOpen.setOnClickListener(v -> openOtpDialog());
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
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Sử dụng LocalDate (Chỉ có ngày tháng năm, không có giờ phút giây)
                java.time.LocalDate now = java.time.LocalDate.now();
                java.time.LocalDate futureDate = now.plusMonths(months);

                // Chuyển về Date để lưu vào Firestore
                java.time.ZonedDateTime zdt = futureDate.atStartOfDay(java.time.ZoneId.systemDefault());
                maturityDate = java.util.Date.from(zdt.toInstant());

                // Format hiển thị
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                binding.tvMaturityDate.setText(futureDate.format(formatter));
            } else {
                // Dùng cách Calendar đã sửa ở trên cho các máy đời cũ
                Calendar cal = Calendar.getInstance();

                // 🔹 QUAN TRỌNG: Đưa về 00:00:00:00 để chuẩn hóa ngày (Tránh lệch giờ phút giây)
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                months = 0;
                if (term.contains("3")) months = 3;
                else if (term.contains("6")) months = 6;
                else if (term.contains("12")) months = 12;
                else if (term.contains("24")) months = 24;

                if (months == 0) {
                    binding.tvMaturityDate.setText("Không thời hạn");
                    maturityDate = null;
                } else {
                    // 🔹 Dùng add(MONTH) là đúng, nhưng cần lưu ý:
                    // Nếu hôm nay là 31/01, cộng 1 tháng sẽ ra 28/02 (chuẩn ngân hàng)
                    cal.add(Calendar.MONTH, months);

                    maturityDate = cal.getTime();

                    binding.tvMaturityDate.setText(
                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    .format(maturityDate)
                    );
                }
            }
        }
    }

    private void calculateProfit() {
        String raw = binding.edtAmount.getText().toString().replaceAll("[^\\d]", "");
        if (raw.isEmpty() || rate == 0) {
            binding.tvEstimatedProfit.setText("0 VND");
            return;
        }

        double amount = Double.parseDouble(raw);
        // Formula: Profit = (Principal * Annual Rate / 100) / 12 * Months
        profit = (amount * rate / 100) / 12 * months;

        binding.tvEstimatedProfit.setText(
                String.format("%,.0f VND", profit)
        );
    }

    // ================= OTP =================

    private void openOtpDialog() {

        String raw = binding.edtAmount.getText().toString().replaceAll("[^\\d]", "");
        if (raw.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        OtpDialogFragment dialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                // Sử dụng số tiền đã làm sạch định dạng để truyền vào hàm giao dịch
                openSavingAtomic(Double.parseDouble(raw));
            }

            @Override
            public void onOtpFailed() {
                Toast.makeText(open_savings.this,
                        "Xác thực thất bại", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show(getSupportFragmentManager(), "OTP_DIALOG");
    }

    // ================= ATOMIC TRANSACTION =================

    // Hàm giả lập tạo số tài khoản. Thực tế cần logic phức tạp hơn.
    private String generateAccountNumber(String prefix) {
        return prefix + UUID.randomUUID().toString().replaceAll("[^\\d]", "").substring(0, 8);
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
                        Toast.makeText(this, "Không tìm thấy tài khoản nguồn", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot checkingDoc = snapshot.getDocuments().get(0);

                    db.runTransaction((Transaction.Function<Void>) transaction -> {
                        DocumentSnapshot freshChecking = transaction.get(checkingDoc.getReference());

                        double currentBalance = freshChecking.getDouble("balance") != null ?
                                freshChecking.getDouble("balance") : 0.0;

                        if (currentBalance >= amount) {
                            // 1. Tính toán số dư mới của tài khoản thanh toán
                            double newBalance = currentBalance - amount;

                            // 2. Cập nhật số dư tài khoản thanh toán
                            transaction.update(checkingDoc.getReference(), "balance", newBalance);

                            // 3. Tạo tài khoản tiết kiệm mới
                            Map<String, Object> savingAccount = new HashMap<>();
                            savingAccount.put("user_id", userId);
                            savingAccount.put("account_number", savingsAccountNumber);
                            savingAccount.put("account_type", "savings");
                            savingAccount.put("balance", amount);
                            savingAccount.put("interest_rate", rate);
                            savingAccount.put("period_months", months);
                            savingAccount.put("status", "active");
                            savingAccount.put("created_at", FieldValue.serverTimestamp()); // Dùng ServerTimestamp cho đồng bộ
                            if (maturityDate != null) {
                                savingAccount.put("maturity_date", maturityDate);
                            }

                            // Tạo Document ID mới cho tài khoản tiết kiệm
                            transaction.set(db.collection("Accounts").document(), savingAccount);

                            // 4. Tạo lịch sử giao dịch (AccountTransactions)
                            String txnId = db.collection("AccountTransactions").document().getId();
                            Map<String, Object> txn = new HashMap<>();
                            txn.put("transactionId", txnId);
                            txn.put("userId", userId);
                            txn.put("type", "OPEN_SAVINGS");
                            txn.put("amount", amount);
                            txn.put("balanceAfter", newBalance);
                            txn.put("status", "SUCCESS");
                            txn.put("timestamp", FieldValue.serverTimestamp());
                            txn.put("senderAccountNumber", freshChecking.getString("account_number"));
                            txn.put("receiverAccountNumber", savingsAccountNumber);
                            txn.put("description", "Mở sổ tiết kiệm kỳ hạn " +
                                    (months == 0 ? "không thời hạn" : months + " tháng"));

                            transaction.set(db.collection("AccountTransactions").document(txnId), txn);

                        } else {
                            throw new RuntimeException("Insufficient balance");
                        }
                        return null;

                    }).addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Toast.makeText(open_savings.this, "Mở tài khoản tiết kiệm thành công!", Toast.LENGTH_LONG).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        showLoading(false);
                        if (e.getMessage() != null && e.getMessage().contains("Insufficient balance")) {
                            Toast.makeText(this, "Số dư tài khoản không đủ.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Lỗi hệ thống: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }
}
