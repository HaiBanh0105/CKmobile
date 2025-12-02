package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class open_savings extends AppCompatActivity {
    private MaterialToolbar toolbar;

    private TextView tvcheckingAmount,tvAppliedRate,tvEstimatedProfit,tvMaturityDate;

    Double rate, profit;

    Date maturityDate;

    private TextInputEditText amount;

    private AutoCompleteTextView autoCompleteTerm;

    String userId = SessionManager.getInstance().getUserId();
    String email = SessionManager.getInstance().getEmail();

    private FirebaseFirestore db;

    MaterialButton btnOpen;

    private ActivityResultLauncher<Intent> launcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.open_savings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.openSaving), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo launcher
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){
                                OpenSaving(userId);
                            }
                        }
                    }
                }
        );

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        autoCompleteTerm = findViewById(R.id.autoCompleteTerm);
        tvcheckingAmount = findViewById(R.id.tvSourceBalance);
        tvAppliedRate = findViewById(R.id.tvAppliedRate);
        tvEstimatedProfit = findViewById(R.id.tvEstimatedProfit);
        tvMaturityDate = findViewById(R.id.tvMaturityDate);
        btnOpen = findViewById(R.id.btnConfirmOpen);
        amount = findViewById(R.id.edtAmount);

        loadCheckingInfor(userId);
        loadInterestRate();
        loadMaturityDate();

        //Nút trở về
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        // Danh sách kỳ hạn
        String[] terms = new String[]{"3 Tháng","6 Tháng", "12 Tháng", "24 Tháng", "Không thời hạn"};

        // Adapter cho AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                terms
        );
        autoCompleteTerm.setAdapter(adapter);

        // Đặt giá trị mặc định
        autoCompleteTerm.setText(terms[0], false);
        String currentValue = autoCompleteTerm.getText().toString();

        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                profit = rate/100 *  Double.parseDouble(amount.getText().toString().trim());
                loadEstimatedProfit();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        autoCompleteTerm.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                loadMaturityDate();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(open_savings.this, otp.class);
                intent.putExtra("email",email);
                launcher.launch(intent);
            }
        });
    }

    //Cập nhật tiền thanh toán giao diện
    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                tvcheckingAmount.setText(String.format("%,.0f VND", balance));
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(open_savings.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Cập nhật lãi suất giao diện
    private void loadInterestRate(){
        db.collection("InterestRates")
                .whereEqualTo("interest_type", "savings") // lọc theo loại lãi suất
                .orderBy("created_at", Query.Direction.DESCENDING) // sắp xếp mới nhất
                .limit(1) // chỉ lấy 1 bản ghi
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Double latestRate = doc.getDouble("interest_rate");
                        rate = latestRate;

                        tvAppliedRate.setText(latestRate.toString() + "% / năm");

                    } else {
                        Toast.makeText(this, "Chưa có dữ liệu lãi suất savings", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi truy vấn: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    //Cập nhật ngày đáo hạn giao diện
    private void loadMaturityDate() {
        // Lấy kỳ hạn từ AutoCompleteTextView
        String termStr = autoCompleteTerm.getText().toString().trim();

        if (!termStr.isEmpty()) {
            // Ngày hiện tại
            Calendar calendar = Calendar.getInstance();

            // Xác định số tháng từ kỳ hạn
            int months = 0;
            if (termStr.contains("3 Tháng")) {
                months = 3;
            } else if (termStr.contains("6 Tháng")) {
                months = 6;
            } else if (termStr.contains("12 Tháng")) {
                months = 12;
            } else if (termStr.contains("24 Tháng")) {
                months = 24;
            }

            // Cộng thêm số tháng vào ngày hiện tại
            calendar.add(Calendar.MONTH, months);
            maturityDate = calendar.getTime();

            // Định dạng ngày đáo hạn
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String maturityDate = sdf.format(calendar.getTime());

            // Hiển thị lên TextView
            if(months != 0){
                tvMaturityDate.setText(maturityDate);
            }
            else{
                tvMaturityDate.setText("Không thời hạn");
            }
        } else {
            tvMaturityDate.setText("Chưa chọn kỳ hạn");
        }
    }


    //Cập nhật lợi nhuận giao diện
    private void loadEstimatedProfit(){
        String formatProfit = String.format("%,.0f VND", profit);
        tvEstimatedProfit.setText(formatProfit);
    }

    private void OpenSaving(String userId){
        // accountId tự động tạo
        String accountId = generateAccountId("02");
        String rawAmount = amount.getText().toString().trim();
        if (rawAmount.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        // Xóa dấu phẩy hoặc chấm ngăn cách hàng nghìn
        String cleanAmount = rawAmount.replaceAll("[^\\d]", "");
        double Amount = Double.parseDouble(cleanAmount);

        String rawChecking = tvcheckingAmount.getText().toString().trim();
        String cleanChecking = rawChecking.replaceAll("[^\\d]", "");
        double checkingAmount = Double.parseDouble(cleanChecking);


        if(Amount > checkingAmount){
            Toast.makeText(this, "Tài khoản thanh toán không đủ, vui lòng nạp thêm tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> account = new HashMap<>();
        account.put("account_id", accountId);
        account.put("user_id", userId);
        account.put("account_type", "savings");
        account.put("balance", Amount); // số dư mặc định = 0
        account.put("created_at", FieldValue.serverTimestamp());
        if(tvMaturityDate.getText() == "Không thời hạn"){
            account.put("maturity_date", "Không thời hạn");
        }else{
            account.put("maturity_date", maturityDate);
            account.put("interest_rate", rate);
        }
        account.put("status", "active");

        db.collection("Accounts").document(accountId).set(account)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tài khoản tiết kiệm đã được tạo thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateAccountId(String type) {
        String branchCode = "7010";      // mã chi nhánh

        // Sinh 6 số ngẫu nhiên
        int randomNumber = (int)(Math.random() * 1000000);
        String randomSixDigits = String.format("%06d", randomNumber);

        // Ghép lại thành accountId
        return branchCode + type + randomSixDigits;
    }

}