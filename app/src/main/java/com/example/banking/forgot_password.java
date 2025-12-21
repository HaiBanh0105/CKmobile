package com.example.banking;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Fragment.OtpDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class forgot_password extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnReset;
    private FirebaseFirestore db;

    // Biến lưu tạm thông tin user tìm được
    private String foundUserId;
    private String foundUserEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgot_password), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        edtEmail = findViewById(R.id.edtEmail);
        btnReset = findViewById(R.id.btnResetPassword);

        // Nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> handleFindUser());
    }

    // BƯỚC 1: Tìm kiếm User theo Email trong Firestore
    private void handleFindUser() {
        String email = edtEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            return;
        }

        // Bật loading hoặc disable nút để tránh spam
        btnReset.setEnabled(false);
        btnReset.setText("Đang kiểm tra...");

        db.collection("Users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Tìm thấy user
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        foundUserId = doc.getId();
                        foundUserEmail = doc.getString("email");
                        String foundPin = doc.getString("pin"); // Giả sử trường lưu PIN là 'pin'

                        SessionManager.getInstance().createLoginSession(
                                foundUserId,
                                doc.getString("fullName"),
                                foundUserEmail,
                                foundPin // Lưu ý: Cần chắc chắn SessionManager có hàm lưu PIN
                        );

                        // BƯỚC 2: Gọi Dialog
                        showOtpDialog();

                    } else {
                        tilEmail.setError("Email không tồn tại trong hệ thống");
                        btnReset.setEnabled(true);
                        btnReset.setText("Gửi yêu cầu");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnReset.setEnabled(true);
                    btnReset.setText("Gửi yêu cầu");
                });
    }

    // BƯỚC 2: Hiển thị Dialog OTP
    private void showOtpDialog() {
        OtpDialogFragment otpDialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                // Xác thực thành công -> Tạo mật khẩu mới
                processResetPassword();
            }

            @Override
            public void onOtpFailed() {
                Toast.makeText(forgot_password.this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                btnReset.setEnabled(true);
                btnReset.setText("Gửi yêu cầu");
            }
        });

        otpDialog.show(getSupportFragmentManager(), "OtpDialog");
    }

    // BƯỚC 3: Tạo mật khẩu mới và gửi Email
    private void processResetPassword() {
        //Tạo mật khẩu ngẫu nhiên
        String newPassword = generateRandomPassword(8);
        String newhash = hashPassword(newPassword);
        //Cập nhật mật khẩu mới lên Firestore

        Map<String, Object> updates = new HashMap<>();
        updates.put("password", newhash); // Ví dụ trường password

        db.collection("Users").document(foundUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 3. Gửi email thông báo mật khẩu mới
                    sendNewPasswordEmail(foundUserEmail, newPassword);

                    // 4. Logout session tạm thời ra để tránh lỗi logic
                    SessionManager.getInstance().logoutUser();

                    // 5. Thông báo và chuyển về Login
                    Toast.makeText(this, "Mật khẩu mới đã được gửi vào email!", Toast.LENGTH_LONG).show();
                    finish(); // Đóng màn hình này
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật mật khẩu", Toast.LENGTH_SHORT).show();
                    btnReset.setEnabled(true);
                    btnReset.setText("Gửi yêu cầu");
                });
    }

    // Hàm gửi email chứa mật khẩu mới
    private void sendNewPasswordEmail(String email, String newPass) {
        String subject = "Cấp lại mật khẩu mới";
        String body = "Mật khẩu mới của bạn là: " + newPass + "\n\nVui lòng đăng nhập và đổi mật khẩu ngay lập tức.";

        EmailService.sendEmail(this, email, subject, body, null);
    }

    // Hàm tạo chuỗi ngẫu nhiên
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}