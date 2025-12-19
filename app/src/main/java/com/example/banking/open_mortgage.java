package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class open_mortgage extends AppCompatActivity {
    private EditText edtLoanAmount, edtYears;
    private TextView tvMonthlyPayment, tvInterestRate;
    private Spinner spinnerPurpose;
    private Button btnSubmit;
    Double rate, monthlyPayment;
    private FirebaseFirestore db;

    String customer_ID, customer_email;

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_open_mortgage);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.open_mortgage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent getIntent = getIntent();
        customer_ID = getIntent.getStringExtra("customer_ID");
        customer_email = getIntent.getStringExtra("email");

        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){
                                OpenMortgage(customer_ID);
                            }
                        }
                    }
                }
        );

        db = FirebaseFirestore.getInstance();

        edtLoanAmount = findViewById(R.id.edtLoanAmount);
        edtYears = findViewById(R.id.edtYears);
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment);
        tvInterestRate = findViewById(R.id.tvInterestRate);
        spinnerPurpose = findViewById(R.id.spinnerPurpose);
        btnSubmit = findViewById(R.id.btnSubmit);

        loadInterestRate();

        String[] purposes = {"Mua nhà", "Mua xe", "Kinh doanh", "Tiêu dùng cá nhân"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>( this, android.R.layout.simple_spinner_item, purposes );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPurpose.setAdapter(adapter);

        edtYears.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                calMonthlyPayment();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        edtLoanAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                calMonthlyPayment();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        btnSubmit.setOnClickListener(v -> {
            String amountStr = edtLoanAmount.getText().toString();
            String yearStr = edtYears.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            else if (yearStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập thới gian", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(open_mortgage.this, otp.class);
            intent.putExtra("email",customer_email);
            launcher.launch(intent);
        });
    }

    private void calMonthlyPayment(){
        try{
            double loanAmount = Double.parseDouble(edtLoanAmount.getText().toString());
            int years = Integer.parseInt(edtYears.getText().toString());

            String rateText = tvInterestRate.getText().toString().replace("%/năm", "").trim();
            double annualRate = Double.parseDouble(rateText) / 100.0;
            // Tính toán
            int months = years * 12;
            double monthlyRate = annualRate / 12.0;
            monthlyPayment = (loanAmount * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -months));
            // Hiển thị kết quả
            tvMonthlyPayment.setText(String.format("%,.0f VND", monthlyPayment));
        } catch (Exception e) {
            tvMonthlyPayment.setText("0 VND");
            return;
        }
    }

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
                        tvInterestRate.setText(latestRate.toString() + "%/năm");

                    } else {
                        Toast.makeText(this, "Chưa có dữ liệu lãi suất savings", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi truy vấn: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void OpenMortgage(String userId){
        // accountId tự động tạo
        String accountId = generateAccountId("03");

        Double Amount = Double.parseDouble(edtLoanAmount.getText().toString());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1); //hạn trả là 1 tháng
        String selectedPurpose = spinnerPurpose.getSelectedItem().toString();

        Map<String, Object> account = new HashMap<>();
        account.put("account_id", accountId);
        account.put("user_id", userId);
        account.put("account_type", "mortgages");
        account.put("purpose", selectedPurpose);
        account.put("balance", Amount);
        account.put("remaining_debt", Amount);
        account.put("monthlyPayment", monthlyPayment);
        account.put("created_at", FieldValue.serverTimestamp());

        account.put("maturity_years", Integer.parseInt(edtYears.getText().toString()));
        account.put("interest_rate", rate);

        account.put("period_day", calendar.getTime());
        account.put("status", "active");

        db.collection("Accounts").document(accountId).set(account)
                .addOnSuccessListener(aVoid -> {
                    FirestoreHelper helper = new FirestoreHelper();
                    helper.changeCheckingBalanceByUserId(this,userId,Amount);
                    Toast.makeText(this, "Tài khoản vay vốn đã được tạo thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateAccountId(String type) {
        String branchCode = "1010";      // mã chi nhánh

        // Sinh 6 số ngẫu nhiên
        int randomNumber = (int)(Math.random() * 1000000);
        String randomSixDigits = String.format("%06d", randomNumber);

        // Ghép lại thành accountId
        return branchCode + type + randomSixDigits;
    }
}