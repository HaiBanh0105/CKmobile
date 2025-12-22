package com.example.officer.Activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.officer.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class interest_rate extends AppCompatActivity {
    private AutoCompleteTextView autoCompleteProductType;
    private TextInputEditText edtInterestRate, edtEffectiveDate;
    private MaterialButton btnSaveConfig;
    private MaterialToolbar toolbar;

    private FirebaseFirestore db;

    TextView tvCurrentRateValue, tvLastUpdated;

    TextInputEditText edtSavingsRate, edtMortgageRate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.interest_rate);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(com.example.officer.R.id.interest_rate), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        autoCompleteProductType = findViewById(R.id.autoCompleteProductType);
        edtInterestRate = findViewById(R.id.edtInterestRate);
        edtEffectiveDate = findViewById(R.id.edtEffectiveDate);
        btnSaveConfig = findViewById(R.id.btnSaveConfig);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvCurrentRateValue = findViewById(R.id.tvCurrentRateValue);

        // Toolbar back
        toolbar.setNavigationOnClickListener(v -> finish());

        String[] productTypes = {"tiết kiệm", "vay vốn"};
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, productTypes);
        autoCompleteProductType.setAdapter(productAdapter);

        loadInterestRate("savings");
        autoCompleteProductType.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String productType = autoCompleteProductType.getText().toString().trim();
                if ("tiết kiệm".equalsIgnoreCase(productType)){
                    loadInterestRate("savings");
                }
                else{
                    loadInterestRate("mortgage");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // 3. DatePicker cho Ngày cập nhật
        edtEffectiveDate.setOnClickListener(v -> showDatePickerDialog(edtEffectiveDate));

        btnSaveConfig.setOnClickListener(v -> saveInterestRate());

        toolbar = findViewById(R.id.toolbar);
        //Nút trở về
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    //    Lưu lãi xuất
    private void saveInterestRate() {
        String productType = autoCompleteProductType.getText().toString().trim();
        String interestRateStr = edtInterestRate.getText().toString().trim();
        String effectiveDate = edtEffectiveDate.getText().toString().trim();

        if (productType.isEmpty() || interestRateStr.isEmpty() || effectiveDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double interestRate;
        try {
            interestRate = Double.parseDouble(interestRateStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Lãi suất phải là số", Toast.LENGTH_SHORT).show();
            return;
        }

        String text;
        if(productType.equalsIgnoreCase("tiết kiệm")){
            text = "savings";
        }else{
            text ="mortgage";
        }


        // Tạo dữ liệu để lưu
        Map<String, Object> data = new HashMap<>();
        data.put("interest_type", text);
        data.put("interest_rate", interestRate);
        data.put("effective_date", effectiveDate);
        data.put("created_at", FieldValue.serverTimestamp());

        // Lưu lên Firestore
        db.collection("InterestRates")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(this, "Đã lưu lãi suất thành công", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        finish();
    }
    // Hàm hiển thị DatePickerDialog
    private void showDatePickerDialog(TextInputEditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    targetEditText.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void loadInterestRate(String type){
        db.collection("InterestRates")
                .whereEqualTo("interest_type", type)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        Double latestRate = querySnapshots.getDocuments().get(0).getDouble("interest_rate");
                        java.util.Date created_at = querySnapshots.getDocuments().get(0).getDate("created_at");
                        tvCurrentRateValue.setText(String.format("%.1f", latestRate) + "% / năm");
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvLastUpdated.setText("Cập nhật lần cuối: "+sdf.format(created_at));

                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải lãi suất " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}