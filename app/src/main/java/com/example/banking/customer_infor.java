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

import com.example.banking.Fragment.OtpDialogFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class customer_infor extends AppCompatActivity {

    private EditText edtFullName, edtPhoneNumber, edtEmail, edtAddress, edtIdCard;
    private MaterialButton btnEkycScan;
    private AppCompatButton btnSave;
    private MaterialToolbar toolbar;
    private TextInputLayout tilIdCard;

    private String faceImagePath, customer_ID;
    private String old_name, old_phone, old_email, old_address;

    // [THAY ĐỔI 1] Chỉ giữ lại ekycLauncher, xóa otpLauncher
    private ActivityResultLauncher<Intent> ekycLauncher;

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
        Intent getIntent = getIntent();
        String role = getIntent.getStringExtra("role");
        customer_ID = getIntent.getStringExtra("customer_ID");

        if ("customer_register".equalsIgnoreCase(role)) {
            toolbar.setTitle("Đăng ký tài khoản");
        } else {
            tilIdCard.setVisibility(View.GONE);

            if (customer_ID != null && !customer_ID.isEmpty()) {
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
        // [THAY ĐỔI 2] Xóa phần register otpLauncher cũ đi
        ekycLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        faceImagePath = result.getData().getStringExtra("faceImagePath");
                        btnEkycScan.setText("Đã quét khuôn mặt ✔");
                    }
                });
    }

    private void setupActions() {
        btnEkycScan.setOnClickListener(v -> {
            Intent i = new Intent(this, ekyc.class);
            i.putExtra("type", "create");
            ekycLauncher.launch(i);
        });

        // [THAY ĐỔI 3] Sửa logic nút Lưu
        btnSave.setOnClickListener(v -> {
            String role = getIntent().getStringExtra("role");

            if ("customer_register".equalsIgnoreCase(role)) {

                checkDuplicateAndRegister();
            } else {
                // Nếu là Cập nhật thông tin
                if (!isUpdate() && faceImagePath == null) {
                    Toast.makeText(this, "Không có thông tin thay đổi", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Gọi Dialog OTP thay vì Intent cũ
                showOtpDialog();
            }
        });
    }


    private void showOtpDialog() {
        OtpDialogFragment otpDialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                // OTP đúng -> Tiến hành cập nhật
                updateCustomer();
            }

            @Override
            public void onOtpFailed() {
                // Hủy hoặc sai quá nhiều lần -> Không làm gì hoặc thông báo
                Toast.makeText(customer_infor.this, "Hủy cập nhật thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        // Hiển thị Dialog
        otpDialog.show(getSupportFragmentManager(), "OtpUpdateCustomer");
    }

    // ================= CHECK TRÙNG (GIỮ NGUYÊN) =================
    private void checkDuplicateAndRegister() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String idCard = edtIdCard.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || idCard.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (faceImagePath == null) {
            Toast.makeText(this, "Vui lòng quét khuôn mặt", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(idCard).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(this, "CCCD đã tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("Users").whereEqualTo("phone", phone).get().addOnSuccessListener(qsPhone -> {
                if (!qsPhone.isEmpty()) {
                    Toast.makeText(this, "Số điện thoại đã tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("Users").whereEqualTo("email", email).get().addOnSuccessListener(qsEmail -> {
                    if (!qsEmail.isEmpty()) {
                        Toast.makeText(this, "Email đã tồn tại", Toast.LENGTH_SHORT).show();
                    } else {
//                        registerCustomer();
                        showRegisterOtpDialog(email, idCard, name);
                    }
                });
            });
        });
    }

    private void showRegisterOtpDialog(String email, String tempId, String tempName) {

        SessionManager.getInstance().createLoginSession(
                tempId,
                tempName,
                email,
                "000000"
        );
        OtpDialogFragment otpDialog = new OtpDialogFragment(new OtpDialogFragment.OtpCallback() {
            @Override
            public void onOtpSuccess() {
                registerCustomer();
            }

            @Override
            public void onOtpFailed() {
                Toast.makeText(customer_infor.this, "Xác thực email thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                SessionManager.getInstance().logoutUser();
            }
        });

        otpDialog.setRegisterMode(true);

        otpDialog.show(getSupportFragmentManager(), "OtpRegisterVerify");
    }

    // ================= REGISTER =================
    // ================= REGISTER (ĐÃ CẬP NHẬT GỬI MAIL) =================
    private void registerCustomer() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhoneNumber.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String idCard = edtIdCard.getText().toString().trim();

        // 1. Tạo mật khẩu và PIN ngẫu nhiên
        String rawPass = generateRandomPassword(); // Mật khẩu gốc để gửi mail
        String rawPin = generateRandomPin();       // PIN gốc để gửi mail

        List<Float> embedding;
        try {
            embedding = extractFaceEmbedding(this, faceImagePath);
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi sinh trắc học", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Tạo Map dữ liệu User
        Map<String, Object> user = new HashMap<>();
        user.put("user_id", idCard);
        user.put("name", name);
        user.put("phone", phone);
        user.put("email", email);
        user.put("address", address);
        user.put("role", "customer");

        // Lưu mật khẩu đã mã hóa vào DB (bảo mật)
        user.put("password", hashPassword(rawPass));
        user.put("pin", rawPin); // Lưu PIN (thường cũng nên hash, nhưng ở đây tạm lưu text)
        user.put("avatar", "");

        // [QUAN TRỌNG] Đánh dấu là lần đăng nhập đầu tiên
        user.put("is_first_login", true);

        // 3. Lưu vào Firestore
        db.collection("Users").document(idCard).set(user).addOnSuccessListener(v -> {
            createDefaultCheckingAccount(idCard);

            Map<String, Object> face = new HashMap<>();
            face.put("user_id", idCard);
            face.put("faceEmbedding", embedding);
            face.put("time", FieldValue.serverTimestamp());
            db.collection("faceId").document(idCard).set(face);

            // [MỚI] Gửi email chứa mật khẩu & PIN cho khách
            sendWelcomeEmail(email, name, idCard, rawPass, rawPin);

            Toast.makeText(this, "Đăng ký thành công! Đã gửi mật khẩu về email.", Toast.LENGTH_LONG).show();

            // Đóng màn hình, quay về login
            finish();
        });
    }

    // ================= GỬI EMAIL THÔNG BÁO =================
    private void sendWelcomeEmail(String toEmail, String name, String username, String password, String pin) {
        String subject = "Chào mừng bạn đến với Ngân hàng số - Đăng ký thành công";

        String body = "Xin chào " + name + ",\n\n" +
                "Chúc mừng bạn đã đăng ký tài khoản thành công.\n" +
                "Dưới đây là thông tin đăng nhập của bạn:\n\n" +
                "--------------------------------\n" +
                "👤 Tên đăng nhập (CCCD): " + username + "\n" +
                "🔑 Mật khẩu tạm thời: " + password + "\n" +
                "🔢 Mã PIN giao dịch: " + pin + "\n" +
                "--------------------------------\n\n" +
                "⚠️ YÊU CẦU QUAN TRỌNG:\n" +
                "Vì lý do bảo mật, vui lòng đăng nhập và ĐỔI MẬT KHẨU + MÃ PIN ngay lập tức.\n\n" +
                "Xin cảm ơn đã sử dụng dịch vụ của chúng tôi!";

        // Gọi EmailService (đảm bảo bạn đã có class này từ các bước trước)
        EmailService.sendEmail(this, toEmail, subject, body, null);
    }

    // ================= UPDATE (GIỮ NGUYÊN) =================
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

        if (faceImagePath != null && !faceImagePath.trim().isEmpty()) {
            final List<Float> faceEmbedding;
            try {
                faceEmbedding = extractFaceEmbedding(this, faceImagePath);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> faceID_update = new HashMap<>();
            faceID_update.put("faceEmbedding", faceEmbedding);
            faceID_update.put("time", FieldValue.serverTimestamp());

            db.collection("faceId").document(id).update(faceID_update);
        }
    }

    private boolean isUpdate() {
        if (old_name == null) return true;
        return !edtFullName.getText().toString().equals(old_name)
                || !edtPhoneNumber.getText().toString().equals(old_phone)
                || !edtEmail.getText().toString().equals(old_email)
                || !edtAddress.getText().toString().equals(old_address);
    }

    // ================= LOAD DATA & UTILS (GIỮ NGUYÊN) =================
    private void loadCustomerInfor(String id) {
        new FirestoreHelper().loadCustomerInfor(id, new FirestoreHelper.CustomerCallback() {
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

    private List<Float> normalizeEmbedding(float[] embeddingArray) {
        double norm = 0.0;
        for (float v : embeddingArray) norm += v * v;
        norm = Math.sqrt(norm);

        List<Float> normalized = new ArrayList<>(embeddingArray.length);
        if (norm == 0) {
            for (float v : embeddingArray) normalized.add(v);
            return normalized;
        }

        for (float v : embeddingArray) normalized.add((float)(v / norm));
        return normalized;
    }

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
}