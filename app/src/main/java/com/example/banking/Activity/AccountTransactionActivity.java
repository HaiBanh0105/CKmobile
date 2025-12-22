package com.example.banking.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Fragment.OtpDialogFragment;
import com.example.banking.R;
import com.example.banking.SessionManager;
import com.example.banking.customer_main;
import com.example.banking.databinding.ActivityAccountTransactionBinding;
import com.example.banking.ekyc;
import com.example.banking.model.Account;
import com.example.banking.model.AccountTransaction;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class AccountTransactionActivity extends BaseSecureActivity {

    private ActivityAccountTransactionBinding binding;
    private FirebaseFirestore db;
    private AccountTransaction transaction;
    private String transactionId;
    private String userId = SessionManager.getInstance().getUserId();
    private ActivityResultLauncher<Intent> ekycLauncher;

    private enum ConfirmAction { CONFIRM, RETRY, GO_HOME }
    private ConfirmAction confirmAction = ConfirmAction.CONFIRM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAccountTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        transactionId = getIntent().getStringExtra("TRANSACTION_ID");

        if (transactionId == null) {
            Toast.makeText(this, "Không tìm thấy mã giao dịch", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initLaunchers();
        listenTransaction();
        setupActions();
    }

    private void initLaunchers() {
        ekycLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && "OK".equalsIgnoreCase(data.getStringExtra("result_key"))) {
                            db.collection("Users").document(userId)
                                    .update("pin_fail_count", 0, "otp_fail_count", 0, "ekyc_required", false)
                                    .addOnSuccessListener(aVoid -> showLoading(false))
                                    .addOnFailureListener(e -> showLoading(false));
                            showOtpDialog();
                        } else {
                            failTransaction("Xác thực khuôn mặt thất bại hoặc bị hủy");
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Xác thực khuôn mặt thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void listenTransaction() {
        db.collection("AccountTransactions")
                .document(transactionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    transaction = snapshot.toObject(AccountTransaction.class);
                    if (transaction != null) bindData();
                    else {
                        Toast.makeText(this, "Giao dịch không tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void bindData() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(transaction.getAmount());

        binding.tvConfirmDescription.setText(transaction.getDescription());
        String status = transaction.getStatus();

        if ("SUCCESS".equals(status)) {
            confirmAction = ConfirmAction.GO_HOME;
            binding.tvHeader.setText("Giao dịch thành công");
            binding.imgSuccessTop.setVisibility(android.view.View.VISIBLE);
            binding.tvConfirmAmount.setText("-" + formattedPrice);
            binding.tvConfirmAmount.setTextColor(Color.WHITE);
            binding.tvAmountLabel.setTextColor(Color.WHITE);
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_success);
            binding.btnConfirmPayment.setText("Về trang chủ");
            binding.btnConfirmPayment.setBackgroundColor(Color.parseColor("#2D3436"));
            binding.btnCancel.setVisibility(android.view.View.GONE);
            binding.tvConfirmDescription.setText(transaction.getDescription() + "\nMã GD: " + transaction.getTransactionId());

        } else if ("FAILED".equals(status)) {
            confirmAction = ConfirmAction.RETRY;
            binding.tvHeader.setText("Giao dịch thất bại");
            binding.imgSuccessTop.setVisibility(android.view.View.GONE);
            binding.tvConfirmAmount.setTextColor(Color.RED);
            binding.btnConfirmPayment.setText("Thử lại");
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_light);

        } else {
            confirmAction = ConfirmAction.CONFIRM;
            binding.tvHeader.setText("Xác nhận giao dịch");
            binding.imgSuccessTop.setVisibility(android.view.View.GONE);
            binding.tvConfirmAmount.setText("-" + formattedPrice);
            binding.tvConfirmAmount.setTextColor(Color.BLACK);
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_light);
        }

        binding.tvRecipientName.setText(transaction.getReceiverName() != null ? transaction.getReceiverName() : "N/A");

        if (transaction.getReceiverBankName() != null && !transaction.getReceiverBankName().isEmpty()) {
            binding.layoutRecipientBank.setVisibility(android.view.View.VISIBLE);
            binding.dividerBank.setVisibility(android.view.View.VISIBLE);
            binding.tvRecipientBank.setText(transaction.getReceiverBankName());
        } else {
            binding.layoutRecipientBank.setVisibility(android.view.View.GONE);
            binding.dividerBank.setVisibility(android.view.View.GONE);
        }

        if (transaction.getReceiverAccountNumber() != null && !transaction.getReceiverAccountNumber().isEmpty()) {
            binding.layoutRecipientAccount.setVisibility(android.view.View.VISIBLE);
            binding.dividerAccount.setVisibility(android.view.View.VISIBLE);
            binding.tvRecipientAccount.setText(transaction.getReceiverAccountNumber());
        } else {
            binding.layoutRecipientAccount.setVisibility(android.view.View.GONE);
            binding.dividerAccount.setVisibility(android.view.View.GONE);
        }

        String displayService = getTransactionTypeVi(transaction.getType());

        binding.tvConfirmType.setText(displayService);

        if (transaction.getBalanceAfter() != null) {
            binding.tvConfirmBalanceAfter.setText(formatter.format(transaction.getBalanceAfter()));
            binding.layoutBalanceAfter.setVisibility(android.view.View.VISIBLE);
            binding.dividerBalanceAfter.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.layoutBalanceAfter.setVisibility(android.view.View.GONE);
            binding.dividerBalanceAfter.setVisibility(android.view.View.GONE);
        }
    }

    private String getCategoryVi(String category) {
        if (category == null) return "Dịch vụ khác";
        switch (category) {
            case "FLIGHT": return "Vé máy bay";
            case "HOTEL": return "Khách sạn";
            case "MOVIE": return "Vé xem phim";
            case "BILL": return "Thanh toán hóa đơn";
            case "SHOPPING": return "Mua sắm";
            case "FOOD": return "Ăn uống";
            default: return "Dịch vụ";
        }
    }

    private String getTransactionTypeVi(String type) {
        if (type == null) return "Giao dịch";
        switch (type) {
            case "TRANSFER_OUT": return "Chuyển khoản";
            case "SERVICE": return "Thanh toán dịch vụ";
            case "BILL": return "Thanh toán hoá đơn";
            default: return "Giao dịch";
        }
    }

    // ================= SECURITY =================
    private void handleSecurity() {
        showLoading(true);
        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    boolean ekycRequired = doc.getBoolean("ekyc_required") != null && doc.getBoolean("ekyc_required");
                    if (ekycRequired || Boolean.TRUE.equals(transaction.getBiometricRequired())) {
                        launchEkyc();
                    } else {
                        showLoading(false);
                        showOtpDialog();
                    }
                }).addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Không kiểm tra được bảo mật", Toast.LENGTH_SHORT).show();
                });
    }

    private void launchEkyc() {
        Intent intent = new Intent(this, ekyc.class);
        intent.putExtra("type","confirm");
        ekycLauncher.launch(intent);
    }

    private void showOtpDialog() {
        OtpDialogFragment otpDialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() { finalizeTransactionAtomic(); }
            @Override
            public void onOtpFailed() { showLoading(false); }
        });
        otpDialog.show(getSupportFragmentManager(), "OTP_DIALOG");
    }

    // ================= ATOMIC TRANSACTION =================
    private void finalizeTransactionAtomic() {
        showLoading(true);
        String accountType = "checking";

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", accountType)
                .limit(1)
                .get()
                .addOnSuccessListener(senderSnap -> {
                    if (senderSnap.isEmpty()) { failTransaction("Không tìm thấy tài khoản thanh toán"); return; }
                    DocumentReference senderRef = senderSnap.getDocuments().get(0).getReference();

                    if ("PAY_MORTGAGE".equals(transaction.getType())) {
                        fetchMortgageAndRunTransaction(senderRef);
                        return;
                    }

                    if (transaction.getReceiverAccountNumber() != null && !transaction.getReceiverAccountNumber().isEmpty()) {
                        db.collection("Accounts")
                                .whereEqualTo("account_number", transaction.getReceiverAccountNumber())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(receiverSnap -> {
                                    if (receiverSnap.isEmpty()) { failTransaction("Không tìm thấy tài khoản người nhận"); return; }
                                    DocumentReference receiverRef = receiverSnap.getDocuments().get(0).getReference();
                                    fetchRelatedDocRefsAndRunTransaction(senderRef, receiverRef);
                                })
                                .addOnFailureListener(e -> failTransaction("Lỗi tra cứu tài khoản người nhận"));
                    } else {
                        fetchRelatedDocRefsAndRunTransaction(senderRef, null);
                    }

                }).addOnFailureListener(e -> failTransaction("Lỗi kết nối hệ thống"));
    }

    private void fetchMortgageAndRunTransaction(DocumentReference senderRef) {
        db.collection("Accounts")
                .whereEqualTo("account_number", transaction.getReceiverAccountNumber())
                .limit(1)
                .get()
                .addOnSuccessListener(mortgageSnap -> {
                    if (mortgageSnap.isEmpty()) {
                        failTransaction("Không tìm thấy khoản vay");
                        return;
                    }

                    DocumentReference mortgageRef =
                            mortgageSnap.getDocuments().get(0).getReference();

                    DocumentReference txRef =
                            db.collection("AccountTransactions").document(transactionId);

                    runMortgageAtomicTransaction(senderRef, mortgageRef, txRef);
                })
                .addOnFailureListener(e -> failTransaction("Lỗi tra cứu khoản vay"));
    }

    private void runMortgageAtomicTransaction(
            DocumentReference senderRef,
            DocumentReference mortgageRef,
            DocumentReference txRef
    ) {
        db.runTransaction(t -> {

                    // ===== 1. READ ALL =====
                    DocumentSnapshot senderSnap = t.get(senderRef);
                    Account sender = senderSnap.toObject(Account.class);

                    if (sender == null || sender.getBalance() == null)
                        throw new FirebaseFirestoreException(
                                "Không đọc được tài khoản thanh toán",
                                FirebaseFirestoreException.Code.ABORTED
                        );

                    DocumentSnapshot mortgageSnap = t.get(mortgageRef);

                    Double remainingDebt = mortgageSnap.getDouble("remaining_debt");
                    Double monthlyPayment = mortgageSnap.getDouble("monthly_payment");
                    Long paidMonths = mortgageSnap.getLong("paid_months");
                    Long totalMonths = mortgageSnap.getLong("total_months");
                    String loanStatus = mortgageSnap.getString("status");
                    Timestamp nextPaymentTs = mortgageSnap.getTimestamp("next_payment_date");

                    if (remainingDebt == null || monthlyPayment == null
                            || paidMonths == null || totalMonths == null
                            || nextPaymentTs == null)
                        throw new FirebaseFirestoreException(
                                "Dữ liệu khoản vay không hợp lệ",
                                FirebaseFirestoreException.Code.ABORTED
                        );

                    if (!"ACTIVE".equals(loanStatus))
                        throw new FirebaseFirestoreException(
                                "Khoản vay không còn hiệu lực",
                                FirebaseFirestoreException.Code.ABORTED
                        );

                    double amount = transaction.getAmount();

                    // ===== 2. VALIDATE =====
                    if (amount <= 0 || amount > remainingDebt)
                        throw new FirebaseFirestoreException(
                                "Số tiền thanh toán không hợp lệ",
                                FirebaseFirestoreException.Code.ABORTED
                        );

                    if (sender.getBalance() < amount)
                        throw new FirebaseFirestoreException(
                                "Số dư không đủ",
                                FirebaseFirestoreException.Code.ABORTED
                        );

                    // ===== 3. CALC =====
                    double senderNewBalance = sender.getBalance() - amount;
                    double newDebt = remainingDebt - amount;
                    long newPaidMonths = paidMonths + 1;

                    boolean isCompleted = newDebt <= 0 || newPaidMonths >= totalMonths;

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(nextPaymentTs.toDate());
                    cal.add(Calendar.MONTH, 1);

                    // ===== 4. WRITE =====
                    t.update(senderRef, "balance", senderNewBalance);

                    if (isCompleted) {
                        t.update(mortgageRef,
                                "remaining_debt", 0,
                                "paid_months", newPaidMonths,
                                "status", "COMPLETED"
                        );
                    } else {
                        t.update(mortgageRef,
                                "remaining_debt", newDebt,
                                "paid_months", newPaidMonths,
                                "next_payment_date", cal.getTime()
                        );
                    }

                    t.update(txRef,
                            "status", "SUCCESS",
                            "balanceBefore", sender.getBalance(),
                            "balanceAfter", senderNewBalance,
                            "timestamp", Timestamp.now()
                    );

                    return null;
                })
                .addOnSuccessListener(r -> showLoading(false))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    failTransaction(e.getMessage());
                });
    }

    private void fetchRelatedDocRefsAndRunTransaction(DocumentReference senderRef,
                                                      @Nullable DocumentReference receiverRef) {

        DocumentReference txRef = db.collection("AccountTransactions").document(transactionId);

        if ("SERVICE".equals(transaction.getType())) {
            db.collection("ServiceBookings")
                    .whereEqualTo("transactionId", transactionId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        DocumentReference relatedRef = snapshot.isEmpty() ? null : snapshot.getDocuments().get(0).getReference();
                        runFullAtomicTransaction(senderRef, receiverRef, txRef, relatedRef);
                    });

        } else if ("BILL".equals(transaction.getType())) {
            db.collection("billing")
                    .whereEqualTo("transactionId", transactionId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        DocumentReference relatedRef = snapshot.isEmpty() ? null : snapshot.getDocuments().get(0).getReference();
                        runFullAtomicTransaction(senderRef, receiverRef, txRef, relatedRef);
                    });

        } else {
            runFullAtomicTransaction(senderRef, receiverRef, txRef, null);
        }
    }


    private void runFullAtomicTransaction(DocumentReference senderRef,
                                          @Nullable DocumentReference receiverRef,
                                          DocumentReference txRef,
                                          @Nullable DocumentReference relatedDocRef) {

        db.runTransaction(t -> {
                    Account sender = t.get(senderRef).toObject(Account.class);
                    if (sender == null || sender.getBalance() == null)
                        throw new FirebaseFirestoreException("Dữ liệu người gửi lỗi", FirebaseFirestoreException.Code.ABORTED);

                    Account receiver = null;
                    if (receiverRef != null) {
                        receiver = t.get(receiverRef).toObject(Account.class);
                        if (receiver == null || receiver.getBalance() == null)
                            throw new FirebaseFirestoreException("Dữ liệu người nhận lỗi", FirebaseFirestoreException.Code.ABORTED);
                    }

                    double amount = transaction.getAmount();
                    if (sender.getBalance() < amount)
                        throw new FirebaseFirestoreException("Số dư không đủ", FirebaseFirestoreException.Code.ABORTED);

                    double senderNewBalance = sender.getBalance() - amount;
                    t.update(senderRef, "balance", senderNewBalance);

                    if (receiver != null && receiverRef != null) {
                        double receiverNewBalance = receiver.getBalance() + amount;
                        t.update(receiverRef, "balance", receiverNewBalance);
                        String receiverUserId = receiver.getUser_id();
                        AccountTransaction receiverTx = cloneTransactionForReceiver(transaction, receiverUserId);
                        receiverTx.setBalanceAfter(receiverNewBalance);
                        t.set(db.collection("AccountTransactions").document(receiverTx.getTransactionId()), receiverTx);
                    }

                    t.update(txRef,
                            "status", "SUCCESS",
                            "balanceBefore", sender.getBalance(),
                            "balanceAfter", senderNewBalance,
                            "timestamp", Timestamp.now());

                    if (relatedDocRef != null) {
                        if ("SERVICE".equals(transaction.getType())) {
                            t.update(relatedDocRef, "status", "SUCCESS", "bookingTime", Timestamp.now());
                        } else if ("BILL".equals(transaction.getType())) {
                            t.update(relatedDocRef, "status", "PAID", "paidAt", Timestamp.now());
                        }
                    }

                    return null;
                }).addOnSuccessListener(r -> showLoading(false))
                .addOnFailureListener(e -> { showLoading(false); failTransaction(e.getMessage()); });
    }

    private AccountTransaction cloneTransactionForReceiver(AccountTransaction original, String receiverUserId) {
        AccountTransaction receiverTx = new AccountTransaction();
        receiverTx.setTransactionId(db.collection("AccountTransactions").document().getId());
        receiverTx.setUserId(receiverUserId); // quan trọng: set userId của người nhận
        receiverTx.setType("TRANSFER_IN");
        receiverTx.setAmount(original.getAmount());
        receiverTx.setDescription(original.getDescription());
        receiverTx.setTimestamp(Timestamp.now());
        receiverTx.setStatus("SUCCESS");

        // Đảo ngược thông tin sender/receiver cho bản ghi của người nhận
        receiverTx.setSenderName(original.getSenderName());
        receiverTx.setSenderAccountNumber(original.getSenderAccountNumber());
        receiverTx.setSenderBankName(original.getSenderBankName());

        receiverTx.setReceiverName(original.getReceiverName());
        receiverTx.setReceiverAccountNumber(original.getReceiverAccountNumber());
        receiverTx.setReceiverBankName(original.getReceiverBankName());

        receiverTx.setCategory(original.getCategory());
        receiverTx.setBiometricRequired(original.getBiometricRequired());
        return receiverTx;
    }


    private void failTransaction(String reason) {
        db.collection("AccountTransactions").document(transactionId)
                .update("status", "FAILED", "description", "Lỗi: " + reason);
        showLoading(false);
        Toast.makeText(this, reason != null ? reason : "Giao dịch thất bại", Toast.LENGTH_LONG).show();
    }

    private void setupActions() {
        binding.btnConfirmPayment.setOnClickListener(v -> {
            switch (confirmAction) {
                case CONFIRM: handleSecurity(); break;
                case RETRY: finish(); break;
                case GO_HOME: goHome(); break;
            }
        });
        binding.btnCancel.setOnClickListener(v -> {
            failTransaction("Người dùng đã hủy giao dịch");
            finish();
        });
    }

    private void goHome() {
        Intent intent = new Intent(this, customer_main.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
