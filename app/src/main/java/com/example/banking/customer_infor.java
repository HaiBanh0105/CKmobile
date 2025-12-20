package com.example.banking;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class customer_infor extends AppCompatActivity {

    private EditText edtFullName, edtPhoneNumber, edtEmail, edtAddress, edtIdCard;
    private MaterialButton btnEkycScan;
    private AppCompatButton btnSave;
    private MaterialToolbar toolbar;
    private TextInputLayout tilIdCard;

    private String faceImagePath, customer_ID;
    private String old_name, old_phone, old_email, old_address;

    private ActivityResultLauncher<Intent> ekycLauncher, otpLauncher;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = SessionManager.getInstance().getUserId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_infor);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.CustomerInfor), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindView();
        setupToolbar();
        setupLaunchers();
        setupActions();
    }

    private void bindView() {
        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdCard = findViewById(R.id.edtIdCard);
        tilIdCard = findViewById(R.id.tilIdCard);

        btnEkycScan = findViewById(R.id.btnEkycScan);
        btnSave = findViewById(R.id.btnSave);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        String role = getIntent().getStringExtra("role");
        customer_ID = getIntent().getStringExtra("customer_ID");

        if ("customer_register".equalsIgnoreCase(role)) {
            toolbar.setTitle("Đăng ký tài khoản");
        } else {
            tilIdCard.setVisibility(View.GONE);

            if (customer_ID != null) {
                toolbar.setTitle("Thông tin khách hàng");
                btnSave.setText("Cập nhật thông tin khách hàng");
                loadCustomerInfor(customer_ID);
            } else {
                toolbar.setTitle("Thông tin tài khoản");
                btnSave.setText("Cập nhật thông tin cá nhân");
                loadCustomerInfor(userId);
            }
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupLaunchers() {
        ekycLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        faceImagePath = result.getData().getStringExtra("faceImagePath");
                        Toast.makeText(this, "Đã quét khuôn mặt", Toast.LENGTH_SHORT).show();
                    }
                });

        otpLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        updateCustomer();
                    }
                });
    }

    private void setupActions() {
        btnEkycScan.setOnClickListener(v -> {
            Intent i = new Intent(this, ekyc.class);
            i.putExtra("type", "create");
            ekycLauncher.launch(i);
        });

        btnSave.setOnClickListener(v -> {
            String role = getIntent().getStringExtra("role");
            if ("customer_register".equalsIgnoreCase(role)) {
                checkDuplicateAndRegister();
            } else {
                if (!isUpdate()) {
                    Toast.makeText(this, "Không có thông tin thay đổi", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(this, otp.class);
                i.putExtra("type", "pin");
                otpLauncher.launch(i);
            }
        });
    }

    // ================= CHECK TRÙNG =================
    private void checkDuplicateAndRegister() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String idCard = edtIdCard.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1️⃣ Check CCCD
        db.collection("Users").document(idCard).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(this, "CCCD đã tồn tại", Toast.LENGTH_SHORT).show();
            } else {
                // 2️⃣ Check SĐT
                db.collection("Users")
                        .whereEqualTo("phone", phone)
                        .get()
                        .addOnSuccessListener(qs -> {
                            if (!qs.isEmpty()) {
                                Toast.makeText(this, "Số điện thoại đã tồn tại", Toast.LENGTH_SHORT).show();
                            } else {
                                registerCustomer();
                            }
                        });
            }
        });
    }

    // ================= REGISTER =================
    private void registerCustomer() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String idCard = edtIdCard.getText().toString().trim();

        if (faceImagePath == null) {
            Toast.makeText(this, "Vui lòng quét khuôn mặt", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawPass = generateRandomPassword();
        String pin = generateRandomPin();

        List<Float> embedding;
        try {
            embedding = extractFaceEmbedding(this, faceImagePath);
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi sinh trắc học", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("user_id", idCard);
        user.put("name", name);
        user.put("phone", phone);
        user.put("email", email);
        user.put("address", address);
        user.put("role", "customer");
        user.put("password", hashPassword(rawPass));
        user.put("pin", pin);
        user.put("avatar", "");

        db.collection("Users").document(idCard).set(user).addOnSuccessListener(v -> {
            createDefaultCheckingAccount(idCard);

            Map<String, Object> face = new HashMap<>();
            face.put("user_id", idCard);
            face.put("faceEmbedding", embedding);
            face.put("time", FieldValue.serverTimestamp());

            db.collection("faceId").document(idCard).set(face);
            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // ================= UPDATE =================
    private void updateCustomer() {
        String id = customer_ID != null ? customer_ID : userId;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", edtFullName.getText().toString().trim());
        updates.put("phone", edtPhoneNumber.getText().toString().trim());
        updates.put("email", edtEmail.getText().toString().trim());
        updates.put("address", edtAddress.getText().toString().trim());

        db.collection("Users").document(id).update(updates);
        Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean isUpdate() {
        if (old_name == null) return true;
        return !edtFullName.getText().toString().equals(old_name)
                || !edtPhoneNumber.getText().toString().equals(old_phone)
                || !edtEmail.getText().toString().equals(old_email)
                || !edtAddress.getText().toString().equals(old_address);
    }

    private void loadCustomerInfor(String id) {
        new FirestoreHelper().loadCustomerInfor(userId, new FirestoreHelper.CustomerCallback() {
            @Override
            public void onSuccess(String name, String phone, String email,
                                  String address, String id, String avatarUrl) {

                edtFullName.setText(name);
                edtPhoneNumber.setText(phone);
                edtEmail.setText(email);
                edtAddress.setText(address);
                edtIdCard.setText(id);

                old_name = name;
                old_phone = phone;
                old_email = email;
                old_address = address;
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(customer_infor.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

    }

    // ================= UTIL =================
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateRandomPin() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void createDefaultCheckingAccount(String userId) {
        String accountId = "101001" + new Random().nextInt(999999);
        Map<String, Object> acc = new HashMap<>();
        acc.put("account_number", accountId);
        acc.put("user_id", userId);
        acc.put("balance", 0.0);
        acc.put("account_type", "checking");
        acc.put("created_at", FieldValue.serverTimestamp());

        db.collection("Accounts").document(accountId).set(acc);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Float> extractFaceEmbedding(Context c, String path) throws IOException {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp == null) throw new IOException();
        FaceEmbeddingExtractor ex = new FaceEmbeddingExtractor(c);
        float[] arr = ex.getEmbedding(bmp);
        ex.close();

        List<Float> list = new ArrayList<>();
        for (float f : arr) list.add(f);
        return list;
    }
}
