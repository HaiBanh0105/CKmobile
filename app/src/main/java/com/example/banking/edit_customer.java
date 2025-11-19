package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class edit_customer extends AppCompatActivity {

    private EditText edtFullName, edtPhoneNumber, edtEmail, edtAddress, edtIdCard;
    private MaterialButton btnEkycScan, btnSave;
    private String faceImagePath; // lưu đường dẫn ảnh khuôn mặt

    private ActivityResultLauncher<Intent> ekycLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer);

        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdCard = findViewById(R.id.edtIdCard);
        btnEkycScan = findViewById(R.id.btnEkycScan);
        btnSave = findViewById(R.id.btnSave);

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

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", name);
        customer.put("phone", phone);
        customer.put("email", email);
        customer.put("address", address);
        customer.put("idCard", idCard);
        customer.put("faceImagePath", faceImagePath); // lưu đường dẫn ảnh khuôn mặt

        db.collection("Customers")
                .add(customer)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Thêm khách hàng thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}