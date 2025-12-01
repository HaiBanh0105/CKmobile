package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class activity_login extends AppCompatActivity {

    private TextInputEditText edtUsername, edtPassword;
    private MaterialButton btnLogin, btnRegister;
    private TextView tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Ánh xạ view
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Xử lý nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            String phone = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String hashedInput = hashPassword(password);


            db.collection("Users")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                            String storedHash = doc.getString("password");
                            String name = doc.getString("name");
                            String role = doc.getString("role");
                            String user_id = doc.getString("user_id");
                            String email = doc.getString("email");

                            if (hashedInput.equals(storedHash)) {
                                SessionManager.getInstance().setUserId(user_id);
                                SessionManager.getInstance().setEmail(email);

                                Toast.makeText(this, "Xin chào " + name, Toast.LENGTH_SHORT).show();

                                if ("staff".equalsIgnoreCase(role)) {
                                    startActivity(new Intent(this, staff_main.class));
                                } else {
                                    startActivity(new Intent(this, customer_main.class));
                                }
                                finish();
                            } else {
                                Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy số điện thoại", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        });

        // Xử lý nút mở tài khoản eKYC
        btnRegister.setOnClickListener(v -> {
//            startActivity(new Intent(this, customer_infor.class));
            Intent intent = new Intent(this, customer_infor.class);

            // Gửi dữ liệu kèm theo
            intent.putExtra("role", "customer_register");
            startActivity(intent);
        });

//        // Xử lý quên mật khẩu
//        tvForgotPassword.setOnClickListener(v -> {
//            String email = edtUsername.getText().toString().trim();
//            if (email.isEmpty()) {
//                Toast.makeText(this, "Nhập email để reset mật khẩu", Toast.LENGTH_SHORT).show();
//            } else {
//                mAuth.sendPasswordResetEmail(email)
//                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã gửi email reset mật khẩu", Toast.LENGTH_SHORT).show())
//                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        });
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