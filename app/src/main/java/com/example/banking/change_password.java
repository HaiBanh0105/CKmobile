package com.example.banking;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class change_password extends AppCompatActivity {
    private TextInputEditText edtCurrentPass, edtNewPass, edtConfirmPass;
    private MaterialButton btnSavePassword;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changePassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        edtCurrentPass = findViewById(R.id.edtCurrentPass);
        edtNewPass = findViewById(R.id.edtNewPass);
        edtConfirmPass = findViewById(R.id.edtConfirmPass);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        db = FirebaseFirestore.getInstance();

        // Bắt sự kiện nút back
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bắt sự kiện nút lưu mật khẩu
        btnSavePassword.setOnClickListener(v -> updatePassword());
    }


    private void updatePassword() {
        String currentPass = edtCurrentPass.getText().toString().trim();
        String newPass = edtNewPass.getText().toString().trim();
        String confirmPass = edtConfirmPass.getText().toString().trim();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy user_id từ SessionManager
        String userId = SessionManager.getInstance().getUserId();

        // Kiểm tra mật khẩu hiện tại trong Firestore
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPass = documentSnapshot.getString("password");

                        String currentHash = hashPassword(currentPass);

                        if (!currentHash.equals(storedPass)) {
                            Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Hash mật khẩu mới trước khi lưu
                        String newHash = hashPassword(newPass);

                        // Cập nhật mật khẩu mới
                        db.collection("Users").document(userId)
                                .update("password", newHash)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                    finish(); // quay lại màn hình trước
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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