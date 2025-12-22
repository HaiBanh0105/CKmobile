package com.example.officer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.officer.Activity.BaseSecureActivity;
import com.example.officer.MainActivity;
import com.example.officer.databinding.ActivityLoginBinding;
import com.example.officer.util.ClickEffectUtil;
import com.example.officer.util.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class login extends BaseSecureActivity {

    private ActivityLoginBinding binding;

    private FirebaseFirestore db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Loading (BaseSecureActivity)
        initLoading(binding.getRoot());

        // Firebase
        db = FirebaseFirestore.getInstance();

        // Hiệu ứng click
        ClickEffectUtil.apply(binding.btnLogin);
        ClickEffectUtil.apply(binding.tvForgotPassword);
        binding.edtUsername.setText("0396930910");
        // ===== ĐĂNG NHẬP =====
        binding.btnLogin.setOnClickListener(v -> {
            String phone = binding.edtUsername.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashedInput = hashPassword(password);

            showLoading(true);
            db.collection("Users")
                    .whereEqualTo("phone", phone)
                    .whereEqualTo("role", "staff")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                            String storedHash = doc.getString("password");
                            String name = doc.getString("name");
                            String pin = doc.getString("pin");
                            String role = doc.getString("role");
                            String userId = doc.getString("user_id");
                            String email = doc.getString("email");
                            String avatar = doc.getString("avatar");

                            if (hashedInput.equals(storedHash)) {

                                SessionManager.getInstance().setUserId(userId);
                                SessionManager.getInstance().setUserName(name);
                                SessionManager.getInstance().setPinNumber(pin);
                                SessionManager.getInstance().setEmail(email);
                                SessionManager.getInstance().setAvatarUrl(avatar);
                                SessionManager.getInstance().setPhone(phone);

                                Toast.makeText(this, "Xin chào " + name, Toast.LENGTH_SHORT).show();

                                if ("staff".equalsIgnoreCase(role))
                                    startActivity(new Intent(this, MainActivity.class));

                                finish();

                            } else {
                                Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                        }
                        showLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });


        //Quên mk
        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, forgot_password.class);
            startActivity(intent);
        });
    }

    // ===== HASH PASSWORD =====
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
