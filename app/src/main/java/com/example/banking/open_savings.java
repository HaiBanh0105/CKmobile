package com.example.banking;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class open_savings extends AppCompatActivity {
    private MaterialToolbar toolbar;

    private TextView tvcheckingAmount,tvAppliedRate,tvEstimatedProfit,tvMaturityDate;

    private TextInputEditText amount;

    private AutoCompleteTextView autoCompleteTerm;

    String userId = SessionManager.getInstance().getUserId();

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

        toolbar = findViewById(R.id.toolbar);
        autoCompleteTerm = findViewById(R.id.autoCompleteTerm);
        tvcheckingAmount = findViewById(R.id.tvSourceBalance);
        tvAppliedRate = findViewById(R.id.tvAppliedRate);
        tvEstimatedProfit = findViewById(R.id.tvEstimatedProfit);
        tvMaturityDate = findViewById(R.id.tvMaturityDate);
        amount = findViewById(R.id.edtAmount);

        loadCheckingInfor(userId);

        //Nút trở về
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });

        // Danh sách kỳ hạn
        String[] terms = new String[]{"6 Tháng", "1 Năm", "2 Năm", "Không thời hạn"};

        // Adapter cho AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                terms
        );
        autoCompleteTerm.setAdapter(adapter);

        // Đặt giá trị mặc định
        autoCompleteTerm.setText(terms[0], false); // "6 Tháng"

        String currentValue = autoCompleteTerm.getText().toString();
    }


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
}