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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class customer_infor extends AppCompatActivity {

    private EditText edtFullName, edtPhoneNumber, edtEmail, edtAddress, edtIdCard;
    private MaterialButton btnEkycScan, btnSave;

    private MaterialToolbar toolbar;
    private String faceImagePath, customer_ID;

    private String old_name, old_phone, old_email, old_address;

    private ActivityResultLauncher<Intent> ekycLauncher;

    String userId = SessionManager.getInstance().getUserId();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customer_infor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.CustomerInfor), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtFullName = findViewById(R.id.edtFullName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdCard = findViewById(R.id.edtIdCard);
        btnEkycScan = findViewById(R.id.btnEkycScan);
        btnSave = findViewById(R.id.btnSave);
        toolbar = findViewById(R.id.toolbar);

        String role = getIntent().getStringExtra("role");
        customer_ID = getIntent().getStringExtra("user_id");

        //Đăng ký tài khoản
        if("customer_register".equalsIgnoreCase(role)){
            toolbar.setTitle("Đăng ký tài khoản");
        }
        else {
            TextInputLayout tilIdCard = findViewById(R.id.tilIdCard);
            tilIdCard.setVisibility(View.GONE);

            //Nhân viên sửa
            if (customer_ID != null && !customer_ID.isEmpty()) {
                toolbar.setTitle("Thông tin khách hàng");
                btnSave.setText("Cập nhật thông tin");
                loadCustomerInfor(customer_ID);
            }
            //Khách hàng tự sửa
            else {
                toolbar.setTitle("Thông tin tài khoản");
                btnSave.setText("Cập nhật thông tin");
                loadCustomerInfor(userId);
            }
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
            intent.putExtra("type","create");
            ekycLauncher.launch(intent);
        });

        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        btnSave.setOnClickListener(v -> {
            if("customer_register".equalsIgnoreCase(role)){
                RegiterCustomer();
            }
            else{
                UpdateCustomer();
            }
        });
    }

    private void RegiterCustomer() {
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

        // Tạo mật khẩu ngẫu nhiên
        String rawPassword = generateRandomPassword();
        // Mã hóa mật khẩu bằng SHA-256
        String hashedPassword = hashPassword(rawPassword);

        //Khởi tạo mã pin ngẫu nhiên
        String rawPin = generateRandomPin();


        final List<Float> faceEmbedding;
        try {
            if (faceImagePath == null || faceImagePath.trim().isEmpty()) {
                Toast.makeText(this, "Vui lòng quét ảnh khuôn mặt", Toast.LENGTH_SHORT).show();
                return;
            }

            File file = new File(faceImagePath);
            if (!file.exists()) {
                Toast.makeText(this, "Ảnh không tồn tại: " + faceImagePath, Toast.LENGTH_SHORT).show();
                return;
            }

            faceEmbedding = extractFaceEmbedding(this,faceImagePath);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return; // dừng lại nếu không tạo được embedding
        }

        // Tiêu đề Email
        String subject = "Chào mừng bạn đến với Ngân hàng ABC";

        // Nội dung Email
        String emailBody = "Xin chào " + name + ",\n\n" +
                "Tài khoản của bạn đã được tạo thành công.\n" +
                "Tên đăng nhập: " + phone + "\n" +
                "Mật khẩu của bạn là: " + rawPassword + "\n\n" +
                "Mã pin của bạn là: " + rawPin + "\n\n" +
                "Vui lòng đổi mật khẩu và mã pin sau khi đăng nhập lần đầu tiên.\n" +
                "Trân trọng,\nNgân hàng ABC.";

        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty() || email.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (faceImagePath == null || faceImagePath.isEmpty()) {
            Toast.makeText(this, "Vui lòng quét sinh trắc học", Toast.LENGTH_SHORT).show();
            return; // dừng lại, không tạo tài khoản
        }


        EmailService.sendEmail(this, email, subject, emailBody, new EmailService.EmailCallback() {
            @Override
            public void onSuccess() {
                Map<String, Object> customer = new HashMap<>();
                customer.put("user_id", idCard);
                customer.put("name", name);
                customer.put("role", "customer");
                customer.put("phone", phone);
                customer.put("email", email);
                customer.put("address", address);
                customer.put("password", hashedPassword);
                customer.put("avatar", "");
                customer.put("pin", rawPin);

                db.collection("Users")
                        .document(idCard)
                        .set(customer )
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getApplicationContext(), "Thêm khách hàng thành công", Toast.LENGTH_SHORT).show();
                            createDefaultCheckingAccount(idCard);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getApplicationContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                Map<String, Object> faceID = new HashMap<>();
                faceID.put("user_id", idCard);
                faceID.put("faceEmbedding", faceEmbedding);
                faceID.put("time", FieldValue.serverTimestamp());

                db.collection("faceId")
                        .document(idCard)
                        .set(faceID);
            }
            @Override
            public void onFailure(String error) {
                Toast.makeText(getApplicationContext(), "Không thể gửi email, hủy tạo tài khoản!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private  void UpdateCustomer(){
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        Boolean isUpdate = false;

        if(!name.equalsIgnoreCase(old_name) || !phone.equalsIgnoreCase(old_phone) ||
            !email.equalsIgnoreCase(old_email) || !address.equalsIgnoreCase(old_address)){
            isUpdate = true;
        }
        String Id_update;
        if(customer_ID != null && !customer_ID.isEmpty()){
            Id_update = customer_ID;
        }
        else{
            Id_update = userId;
        }

        if (isUpdate){
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phone", phone);
            updates.put("email", email);
            updates.put("address", address);

            db.collection("Users").document(Id_update)
                    .update(updates) .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            finish();
        }
        else if(faceImagePath != null && !faceImagePath.trim().isEmpty()){
            Toast.makeText(this, "Cập nhật sinh trắc học thành công", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            Toast.makeText(this, "Không có thông tin thay đổi", Toast.LENGTH_SHORT).show();
        }

        if (faceImagePath != null && !faceImagePath.trim().isEmpty()) {
            //Update faceID
            final List<Float> faceEmbedding;
            try {
                faceEmbedding = extractFaceEmbedding(this, faceImagePath);
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return; // dừng lại nếu không tạo được embedding
            }
            Map<String, Object> faceID_update = new HashMap<>();
            faceID_update.put("faceEmbedding", faceEmbedding);
            faceID_update.put("time", FieldValue.serverTimestamp());

            db.collection("faceId").document(Id_update)
                    .update(faceID_update);
        }
    }


    //Tạo tài khoản checking
    private void createDefaultCheckingAccount(String userId) {
        String name = edtFullName.getText().toString().trim();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // accountId sinh tự động
        String accountId = generateAccountId("01");

        Map<String, Object> account = new HashMap<>();
        account.put("account_number", accountId);
        account.put("user_id", userId);
        account.put("name", name);
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

    //Tạo mật khẩu ngãu nhiên
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateRandomPin() {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    //Tạo mã tự động
    private String generateAccountId(String type) {
        String branchCode = "1010";      // mã chi nhánh

        // Sinh 6 số ngẫu nhiên
        int randomNumber = (int)(Math.random() * 1000000);
        String randomSixDigits = String.format("%06d", randomNumber);

        // Ghép lại thành accountId
        return branchCode + type + randomSixDigits;
    }



    //Mã hóa
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

    //Load dữ liệu lên form khi cập nhật
    private void loadCustomerInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();

        helper.loadCustomerInfor(userId, new FirestoreHelper.CustomerCallback() {
            @Override
            public void onSuccess(String name, String phone, String email, String address, String id, String avatarUrl) {
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

    //Xử lý ảnh khuôn mặt
    private List<Float> extractFaceEmbedding(Context context, String imagePath) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) {
            throw new IOException("Không thể đọc ảnh từ đường dẫn: " + imagePath);
        }

        FaceEmbeddingExtractor extractor = new FaceEmbeddingExtractor(context);
        float[] embeddingArray = extractor.getEmbedding(bitmap);
        extractor.close();

        if (embeddingArray == null || embeddingArray.length == 0) {
            throw new IOException("Không thể trích xuất embedding từ ảnh");
        }

        return normalizeEmbedding(embeddingArray);
    }

    //chuẩn hóa vector
    private List<Float> normalizeEmbedding(float[] embeddingArray) {
        double norm = 0.0;
        for (float v : embeddingArray) norm += v * v;
        norm = Math.sqrt(norm);

        List<Float> normalized = new ArrayList<>(embeddingArray.length);
        for (float v : embeddingArray) normalized.add((float)(v / norm));
        return normalized;
    }

}