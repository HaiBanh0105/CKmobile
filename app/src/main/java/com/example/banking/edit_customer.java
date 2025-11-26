package com.example.banking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class edit_customer extends AppCompatActivity {

    private EditText edtFullName, edtPhoneNumber, edtEmail, edtAddress, edtIdCard;
    private MaterialButton btnEkycScan, btnSave;

    private MaterialToolbar toolbar;
    private String faceImagePath; // lưu đường dẫn ảnh khuôn mặt

    private ActivityResultLauncher<Intent> ekycLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_customer);

        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdCard = findViewById(R.id.edtIdCard);
        btnEkycScan = findViewById(R.id.btnEkycScan);
        btnSave = findViewById(R.id.btnSave);
        toolbar = findViewById(R.id.toolbar);

        String role = getIntent().getStringExtra("role");

        if("customer".equalsIgnoreCase(role)){
            toolbar.setTitle("Đăng ký tài khoản");
        }
        else{
            toolbar.setTitle("Thông tin tài khoản");
        }

        // Đăng ký nhận kết quả từ EkycActivity
        ekycLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        faceImagePath = result.getData().getStringExtra("faceImagePath");
                        Toast.makeText(this, "Đã quét khuôn mặt", Toast.LENGTH_SHORT).show();
                    }
                });

        btnEkycScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, ekyc.class);
            ekycLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveCustomer());
    }

    private void saveCustomer() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String idCard = edtIdCard.getText().toString().trim();


        // Kiểm tra dữ liệu bắt buộc
        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Tạo mật khẩu ngẫu nhiên
        String rawPassword = generateRandomPassword();

        // 2. Mã hóa mật khẩu bằng SHA-256
        String hashedPassword = hashPassword(rawPassword);

        // Tiêu đề Email
        String subject = "Chào mừng bạn đến với Ngân hàng ABC";

        // Nội dung Email
        String emailBody = "Xin chào " + name + ",\n\n" +
                "Tài khoản của bạn đã được tạo thành công.\n" +
                "Tên đăng nhập: " + phone + "\n" +
                "Mật khẩu của bạn là: " + rawPassword + "\n\n" +
                "Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu.\n" +
                "Trân trọng,\nNgân hàng ABC.";

        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (faceImagePath == null || faceImagePath.isEmpty()) {
            Toast.makeText(this, "Vui lòng quét sinh trắc học", Toast.LENGTH_SHORT).show();
            return; // dừng lại, không tạo tài khoản
        }



        // Gọi hàm gửi mail (hàm này tự báo thành công/thất bại bằng Toast)
        EmailService.sendEmail(this, email, subject, emailBody, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                // Chỉ tạo tài khoản khi gửi mail thành công
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> customer = new HashMap<>();
                customer.put("user_id", idCard);
                customer.put("name", name);
                customer.put("role", "customer");
                customer.put("phone", phone);
                customer.put("email", email);
                customer.put("address", address);
                customer.put("faceImagePath", faceImagePath);
                customer.put("password", hashedPassword);

                db.collection("Users")
                        .document(idCard)
                        .set(customer)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getApplicationContext(), "Thêm khách hàng thành công", Toast.LENGTH_SHORT).show();
                            createDefaultCheckingAccount(idCard);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getApplicationContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getApplicationContext(), "Không thể gửi email, hủy tạo tài khoản!", Toast.LENGTH_SHORT).show();
            }
        });

    }


    private void createDefaultCheckingAccount(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String accountId = userId+"_checking";

        Map<String, Object> account = new HashMap<>();
        account.put("account_id", accountId);
        account.put("user_id", userId);
        account.put("account_type", "checking");
        account.put("balance", 0.0); // số dư mặc định = 0
        account.put("created_at", FieldValue.serverTimestamp());

        db.collection("Accounts").document(accountId).set(account)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tài khoản checking mặc định đã được tạo", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
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