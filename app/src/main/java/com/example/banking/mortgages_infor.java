package com.example.banking;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class mortgages_infor extends AppCompatActivity {
    private String account_id;

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

    }

//    private void loadMortgageInfor(String accountId) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        db.collection("Accounts").document(accountId).get()
//                .addOnSuccessListener(doc -> {
//                    if (doc.exists()) {
//                        // Lấy dữ liệu từ document
//                        Double balance = doc.getDouble("balance");
//                        String maturityDate = doc.get("maturity_date") != null ? doc.get("maturity_date").toString() : "";
//                        periodDate = doc.getDate("period_day");
//
//                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
//                        tvPeriodDate.setText(sdf.format(periodDate));
//                        if ("Không thời hạn".equals(maturityDate)) {
//                            // Hiển thị ngày đáo hạn
//                            tvMaturityDate.setText(maturityDate);
//                            // Truy vấn InterestRates để lấy rate mới nhất
//                            db.collection("InterestRates")
//                                    .whereEqualTo("interest_type", "savings")
//                                    .orderBy("created_at", Query.Direction.DESCENDING)
//                                    .limit(1)
//                                    .get()
//                                    .addOnSuccessListener(querySnapshots -> {
//                                        if (!querySnapshots.isEmpty()) {
//                                            Double latestRate = querySnapshots.getDocuments().get(0).getDouble("interest_rate");
//                                            tvSavingsRate.setText(latestRate + "% / năm");
//
//                                            // Tính lợi nhuận tạm tính
//                                            if (balance != null && latestRate != null) {
//                                                estProfit = (balance * latestRate / 100) / 12;
//                                                tvSavingsProfit.setText(String.format("+ %,.0f VND", estProfit));
//                                            }
//                                        }
//                                    })
//                                    .addOnFailureListener(e ->
//                                            tvSavingsRate.setText("Lỗi lấy lãi suất: " + e.getMessage()));
//                        } else {
//                            Date date = doc.getDate("maturity_date");
//                            if (date != null) {
//                                tvMaturityDate.setText(sdf.format(date));
//                            }
//                            // Nếu có kỳ hạn: lấy rate từ document
//                            Double rate = doc.getDouble("interest_rate");
//                            if (rate != null) {
//                                tvSavingsRate.setText(rate + "% / năm");
//                                if (balance != null) {
//                                    estProfit = (balance * rate / 100) / 12;
//                                    tvSavingsProfit.setText(String.format("+ %,.0f VND", estProfit));
//                                }
//                            }
//                        }
//
//                    }
//                })
//                .addOnFailureListener(e ->
//                        tvSavingsRate.setText("Lỗi tải dữ liệu: " + e.getMessage()));
//    }
}