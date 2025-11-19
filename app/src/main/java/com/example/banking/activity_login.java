package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_login extends AppCompatActivity {

    private TextInputEditText edtUsername, edtPassword;
    private MaterialButton btnLogin, btnRegister;
    private TextView tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ánh xạ view
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Xử lý nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            db.collection("Users").document(uid).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            String role = doc.getString("role");
                                            String name = doc.getString("name");

                                            Toast.makeText(this, "Xin chào " + name + " (" + role + ")", Toast.LENGTH_SHORT).show();

                                            if ("staff".equalsIgnoreCase(role)) {
                                                startActivity(new Intent(this, staff_main.class));
//                                                Intent intent = new Intent(this, staff_main.class);
//                                                startActivity(intent);
//                                                finish();

                                            } else {
                                                startActivity(new Intent(this, activity_customer_main.class));
                                            }

                                            finish();
                                        } else {
                                            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

//        // Xử lý nút mở tài khoản eKYC
//        btnRegister.setOnClickListener(v -> {
//            startActivity(new Intent(this, RegisterActivity.class));
//        });

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
}