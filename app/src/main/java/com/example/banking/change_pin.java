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

public class change_pin extends AppCompatActivity {
    private TextInputEditText edtCurrentPin, edtNewPin, edtConfirmPin;
    private MaterialButton btnSavePin;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.change_pin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changePin), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        edtCurrentPin = findViewById(R.id.edtCurrentPin);
        edtNewPin = findViewById(R.id.edtNewPin);
        edtConfirmPin = findViewById(R.id.edtConfirmPin);
        btnSavePin = findViewById(R.id.btnSavePin);

        db = FirebaseFirestore.getInstance();

        // Bắt sự kiện nút back
        toolbar.setNavigationOnClickListener(v -> finish());

        // Bắt sự kiện nút lưu mã pin
        btnSavePin.setOnClickListener(v -> updatePin());
    }


    private void updatePin() {
        String currentPin = edtCurrentPin.getText().toString().trim();
        String newPin = edtNewPin.getText().toString().trim();
        String confirmPin = edtConfirmPin.getText().toString().trim();

        if (currentPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPin.length() != 6) {
            Toast.makeText(this, "Mã pin chỉ 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPin.equals(confirmPin)) {
            Toast.makeText(this, "Mã pin nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy user_id từ SessionManager
        String userId = SessionManager.getInstance().getUserId();

        // Kiểm tra mã pin hiện tại trong Firestore
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPin = documentSnapshot.getString("pin");


                        if (!currentPin.equals(storedPin)) {
                            Toast.makeText(this, "mã pin hiện tại không đúng", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        // Cập nhật mã pin mới
                        db.collection("Users").document(userId)
                                .update("pin", newPin)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đổi mã pin thành công", Toast.LENGTH_SHORT).show();
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


}