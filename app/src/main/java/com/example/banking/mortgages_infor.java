package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class mortgages_infor extends AppCompatActivity {
    private String account_id;

    private TextView tvMortgageRate, tvMortgagePrincipal, tvNextDueDate,  tvMonthlyPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mortgages_infor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mortgages_infor), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent getIntent = getIntent();
        account_id = getIntent.getStringExtra("account_id");

        tvMortgageRate = findViewById(R.id.tvMortgageRate);
        tvMortgagePrincipal = findViewById(R.id.tvMortgagePrincipal);
        tvNextDueDate = findViewById(R.id.tvNextDueDate);
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment);

        loadMortgageInfor(account_id);

    }

    private void loadMortgageInfor(String accountId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Accounts").document(accountId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Lấy dữ liệu từ document
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Double remaining_debt = doc.getDouble("remaining_debt");
                        Double monthlyPayment = doc.getDouble("monthlyPayment");
                        Double interest_rate = doc.getDouble("interest_rate");
                        Date period_day = doc.getDate("period_day");


                        tvMortgageRate.setText(interest_rate + "% / năm");
                        tvMortgagePrincipal.setText(String.format("%,.0f VND", remaining_debt));
                        tvNextDueDate.setText(sdf.format(period_day));
                        tvMonthlyPayment.setText(String.format("%,.0f VND", monthlyPayment));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}