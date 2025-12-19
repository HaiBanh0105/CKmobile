package com.example.banking.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.model.Account;
import com.example.banking.Fragment.OtpDialogFragment;
import com.example.banking.R; // Ensure R is imported correctly
import com.example.banking.SessionManager;
import com.example.banking.customer_main;
import com.example.banking.databinding.ActivityAccountTransactionBinding;
import com.example.banking.ekyc;
import com.example.banking.model.AccountTransaction;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.NumberFormat;
import java.util.Locale;

import javax.annotation.Nullable;

public class AccountTransactionActivity extends BaseSecureActivity  {

    private ActivityAccountTransactionBinding binding;
    private FirebaseFirestore db;

    private AccountTransaction transaction;
    private String transactionId;

    private enum ConfirmAction {
        CONFIRM,
        RETRY,
        GO_HOME
    }
    private ConfirmAction confirmAction = ConfirmAction.CONFIRM;
    private final String userId = SessionManager.getInstance().getUserId();

    private ActivityResultLauncher<Intent> ekycLauncher;

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

    // ================= INIT LAUNCHERS =================
    private void initLaunchers() {
        // eKYC (Xác thực khuôn mặt)
        ekycLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){
                                db.collection("Users")
                                        .document(userId)
                                        .update(
                                                "pin_fail_count", 0,
                                                "otp_fail_count", 0,
                                                "ekyc_required", false
                                        )
                                        .addOnSuccessListener(aVoid -> {
                                            // Reset thành công, tiếp tục bước OTP
                                            showLoading(false);
                                            showOtpDialog();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Không thể reset số lần thất bại", Toast.LENGTH_SHORT).show();
                                            // Vẫn show OTP để user thử tiếp
                                            showLoading(false);
                                            showOtpDialog();
                                        });
                            } else {
                                failTransaction("Xác thực khuôn mặt thất bại hoặc bị hủy");
                            }
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Xác thực khuôn mặt thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= SNAPSHOT LISTENER =================
    private void listenTransaction() {
        db.collection("AccountTransactions")
                .document(transactionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    transaction = snapshot.toObject(AccountTransaction.class);
                    if (transaction == null) return;

                    bindData(); // Chỉ cập nhật dữ liệu lên UI, không tự gọi bảo mật nữa
                });
    }


    // ================= UI BINDING =================
    private void bindData() {
        String status = transaction.getStatus();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedPrice = formatter.format(transaction.getAmount());

        binding.tvConfirmDescription.setText(transaction.getDescription());
        // --- Xử lý giao diện linh hoạt theo trạng thái ---
        if ("SUCCESS".equals(status)) {
            confirmAction = ConfirmAction.GO_HOME;

            // Chế độ Biên lai thành công
            binding.tvHeader.setText("Giao dịch thành công");
            binding.imgSuccessTop.setVisibility(View.VISIBLE); // Icon check lớn
            binding.tvConfirmAmount.setText("-" + formattedPrice);
            binding.tvConfirmAmount.setTextColor(Color.WHITE);
            binding.tvAmountLabel.setTextColor(Color.WHITE);
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_success);

            binding.btnConfirmPayment.setText("Về trang chủ");
            binding.btnConfirmPayment.setBackgroundColor(Color.parseColor("#2D3436"));
            binding.btnCancel.setVisibility(View.GONE);

            // Hiển thị mã giao dịch ở mô tả khi đã thành công
            binding.tvConfirmDescription.setText(transaction.getDescription() + "\nMã GD: " + transaction.getTransactionId());

        } else if ("FAILED".equals(status)) {
            // Chế độ Thất bại
            confirmAction = ConfirmAction.RETRY;
            binding.tvHeader.setText("Giao dịch thất bại");
            binding.imgSuccessTop.setVisibility(View.GONE);
            binding.tvConfirmAmount.setTextColor(Color.RED);
            binding.btnConfirmPayment.setText("Thử lại");
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_light);

        } else {
            confirmAction = ConfirmAction.CONFIRM;

            // Chế độ Chờ xác nhận (PENDING)
            binding.tvHeader.setText("Xác nhận giao dịch");
            binding.imgSuccessTop.setVisibility(View.GONE);
            binding.tvConfirmAmount.setText("-" + formattedPrice);
            binding.tvConfirmAmount.setTextColor(Color.BLACK);
            binding.layoutStatus.setBackgroundResource(R.drawable.bg_status_light);
        }

        // --- Binding thông tin người nhận (ẩn nếu là thanh toán dịch vụ) ---
        if (transaction.getReceiverBankName() != null && !transaction.getReceiverBankName().isEmpty()) {
            binding.layoutRecipientBank.setVisibility(View.VISIBLE);
            binding.dividerBank.setVisibility(View.VISIBLE);
            binding.tvRecipientBank.setText(transaction.getReceiverBankName());
        } else {
            binding.layoutRecipientBank.setVisibility(View.GONE);
            binding.dividerBank.setVisibility(View.GONE);
        }

        if (transaction.getReceiverAccountNumber() != null && !transaction.getReceiverAccountNumber().isEmpty()) {
            binding.layoutRecipientAccount.setVisibility(View.VISIBLE);
            binding.dividerAccount.setVisibility(View.VISIBLE);
            binding.tvRecipientAccount.setText(transaction.getReceiverAccountNumber());
        } else {
            binding.layoutRecipientAccount.setVisibility(View.GONE);
            binding.dividerAccount.setVisibility(View.GONE);
        }

        binding.tvRecipientName.setText(transaction.getReceiverName() != null ? transaction.getReceiverName() : "N/A");
        String displayService;
        if (transaction.getCategory() != null && !transaction.getCategory().isEmpty()) {
            displayService = getCategoryVi(transaction.getCategory());
        } else {
            displayService = getTransactionTypeVi(transaction.getType());
        }

        // Gán vào TextView hiển thị loại giao dịch/dịch vụ
        binding.tvConfirmType.setText(displayService);

        // Cập nhật số dư sau giao dịch (chỉ có sau khi finalize transaction)
        if (transaction.getBalanceAfter() != null) {
            binding.tvConfirmBalanceAfter.setText(formatter.format(transaction.getBalanceAfter()));
            binding.layoutBalanceAfter.setVisibility(View.VISIBLE);
            binding.dividerBalanceAfter.setVisibility(View.VISIBLE);
        } else {
            binding.layoutBalanceAfter.setVisibility(View.GONE);
            binding.dividerBalanceAfter.setVisibility(View.GONE);
        }
    }

    private String getCategoryVi(String category) {
        if (category == null) return "Dịch vụ khác";

        switch (category) {
            case "FLIGHT": return "Vé máy bay";
            case "HOTEL": return "Phách sạn";
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
            case "TRANSFER": return "Chuyển khoản";
            case "SERVICE": return "Thanh toán dịch vụ";
            case "BILL": return "Thanh toán hoá đơn";
            default: return "Giao dịch";
        }
    }

    // ================= SECURITY FLOW =================
    private void handleSecurity() {
        showLoading(true);

        db.collection("Users") // ⚠️ dùng users (chữ thường)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {

                    boolean ekycRequired = doc.getBoolean("ekyc_required") != null
                            && doc.getBoolean("ekyc_required");

                    // 🔒 Nếu bị đánh dấu eKYC → BẮT BUỘC eKYC
                    if (ekycRequired) {
                        launchEkyc();
                        return;
                    }

                    // 🔐 Nếu transaction yêu cầu biometric
                    if (Boolean.TRUE.equals(transaction.getBiometricRequired())) {
                        launchEkyc();
                    } else {
                        showLoading(false);
                        showOtpDialog();
                    }
                })
                .addOnFailureListener(e -> {
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
        // Khởi tạo Dialog
        OtpDialogFragment otpDialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                // Bước cuối cùng: Thực hiện trừ tiền thực tế trong Database
                showLoading(true);
                finalizeTransactionAtomic();
            }

            @Override
            public void onOtpFailed() {
                // Không cần gọi failTransaction ở đây để user có thể bấm "Xác nhận" lại để thử lại
                showLoading(false);
                Toast.makeText(AccountTransactionActivity.this, "Xác thực không thành công", Toast.LENGTH_SHORT).show();
            }
        });

        // Hiển thị Dialog (Bên trong Dialog này đã có logic tự gửi Email khi khởi tạo)
        otpDialog.show(getSupportFragmentManager(), "OTP_DIALOG");
    }


    // ================= ATOMIC FINALIZE (Firestore Transaction) =================
    private void finalizeTransactionAtomic() {
        showLoading(true);

        String userId = SessionManager.getInstance().getUserId();
        String accountType = "checking";

        // 1️⃣ Lấy tài khoản người gửi
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", accountType)
                .limit(1)
                .get()
                .addOnSuccessListener(senderSnap -> {

                    if (senderSnap.isEmpty()) {
                        failTransaction("Không tìm thấy tài khoản thanh toán");
                        return;
                    }

                    DocumentReference senderAccountRef =
                            senderSnap.getDocuments().get(0).getReference();

                    // 2️⃣ Nếu có receiverAccountNumber → query trước (NGOÀI transaction)
                    if (transaction.getReceiverAccountNumber() != null
                            && !transaction.getReceiverAccountNumber().isEmpty()) {

                        db.collection("Accounts")
                                .whereEqualTo(
                                        "account_number",
                                        transaction.getReceiverAccountNumber()
                                )
                                .limit(1)
                                .get()
                                .addOnSuccessListener(receiverSnap -> {

                                    if (receiverSnap.isEmpty()) {
                                        failTransaction("Không tìm thấy tài khoản người nhận");
                                        return;
                                    }

                                    DocumentReference receiverAccountRef =
                                            receiverSnap.getDocuments().get(0).getReference();

                                    // 👉 Chạy transaction có receiver
                                    runAtomicTransaction(senderAccountRef, receiverAccountRef);
                                })
                                .addOnFailureListener(e ->
                                        failTransaction("Lỗi tra cứu tài khoản người nhận"));
                    } else {
                        // 👉 Không có receiver → chỉ trừ tiền
                        runAtomicTransaction(senderAccountRef, null);
                    }
                })
                .addOnFailureListener(e ->
                        failTransaction("Lỗi kết nối hệ thống"));
    }

    private void runAtomicTransaction(
            DocumentReference senderAccountRef,
            @Nullable DocumentReference receiverAccountRef
    ) {
        DocumentReference txRef =
                db.collection("AccountTransactions").document(transactionId);

        db.runTransaction(t -> {

            // ===== 1️⃣ READ TẤT CẢ (BẮT BUỘC TRƯỚC) =====
            Account sender = t.get(senderAccountRef).toObject(Account.class);
            if (sender == null || sender.getBalance() == null) {
                throw new FirebaseFirestoreException(
                        "Dữ liệu người gửi lỗi",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            Account receiver = null;
            if (receiverAccountRef != null) {
                receiver = t.get(receiverAccountRef).toObject(Account.class);
                if (receiver == null || receiver.getBalance() == null) {
                    throw new FirebaseFirestoreException(
                            "Dữ liệu người nhận lỗi",
                            FirebaseFirestoreException.Code.ABORTED
                    );
                }
            }

            // ===== 2️⃣ TÍNH TOÁN =====
            double amount = transaction.getAmount();
            double senderBalance = sender.getBalance();

            if (senderBalance < amount) {
                throw new FirebaseFirestoreException(
                        "Số dư không đủ",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            double senderNewBalance = senderBalance - amount;

            // ===== 3️⃣ WRITE (SAU KHI READ XONG) =====
            t.update(senderAccountRef, "balance", senderNewBalance);

            if (receiverAccountRef != null) {
                double receiverNewBalance = receiver.getBalance() + amount;
                t.update(receiverAccountRef, "balance", receiverNewBalance);
            }

            t.update(
                    txRef,
                    "status", "SUCCESS",
                    "balanceBefore", senderBalance,
                    "balanceAfter", senderNewBalance,
                    "timestamp", Timestamp.now()
            );

            return null;

        }).addOnSuccessListener(r -> {
            showLoading(false);
            handlePostTransactionSuccess();
        }).addOnFailureListener(e -> {
            showLoading(false);
            failTransaction(e.getMessage());
        });
    }


    private void handlePostTransactionSuccess() {
        if (transaction == null || transaction.getType() == null) return;

        switch (transaction.getType()) {
            case "SERVICE":
                updateServiceBookingSuccess();
                break;

            case "BILL":
                updateBillPaymentSuccess();
                break;

            case "TRANSFER":
                // Không cần xử lý thêm
                break;
        }
    }

    private void updateServiceBookingSuccess() {
        db.collection("ServiceBookings")
                .whereEqualTo("transactionId", transactionId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    snapshot.getDocuments()
                            .get(0)
                            .getReference()
                            .update(
                                    "status", "SUCCESS",
                                    "bookingTime", Timestamp.now()
                            );
                });
    }

    private void updateBillPaymentSuccess() {
        db.collection("Bills")
                .whereEqualTo("transactionId", transactionId)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    snapshot.getDocuments()
                            .get(0)
                            .getReference()
                            .update(
                                    "status", "PAID",
                                    "paidAt", Timestamp.now()
                            );
                });
    }

    // ================= FAIL TRANSACTION =================
    private void failTransaction(String reason) {
        // Cập nhật trạng thái FAIL trên DB. Snapshot listener sẽ bắt thay đổi này
        db.collection("AccountTransactions")
                .document(transactionId)
                .update("status", "FAILED", "description", "Lỗi: " + reason);
        showLoading(false);
        Toast.makeText(this, reason != null ? reason : "Giao dịch thất bại", Toast.LENGTH_LONG).show();
        // Không finish() ngay, để người dùng xem màn hình thất bại
    }

    // ================= ACTIONS =================
    private void setupActions() {
        binding.btnConfirmPayment.setOnClickListener(v -> {
            switch (confirmAction) {
                case CONFIRM:
                    handleSecurity();
                    break;

                case RETRY:
                    finish();
                    break;

                case GO_HOME:
                    goHome();
                    break;
            }
        });
        binding.btnCancel.setOnClickListener(v -> {
                failTransaction("Người dùng đã hủy giao dịch");
                finish();
                }
        );
    }

    private void goHome() {
        Intent intent = new Intent(this, customer_main.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
