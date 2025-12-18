package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class saving_infor extends AppCompatActivity {
    private TextView tvSavingsRate, tvSavingsProfit, tvMaturityDate, tvPeriodDate;
    double estProfit;
    String account_Id;
    Date periodDate;

    Button btnWithdraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.savings_infor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.saving_infor), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvSavingsRate = findViewById(R.id.tvSavingsRate);
        tvSavingsProfit = findViewById(R.id.tvSavingsProfit);
        tvMaturityDate = findViewById(R.id.tvMaturityDate);
        tvPeriodDate = findViewById(R.id.tvPeriodDate);
        btnWithdraw = findViewById(R.id.btnWithdraw);


        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        account_Id = intent.getStringExtra("account_id");

        loadSavingInfor(account_Id);

        btnWithdraw.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();

            if(today.before(periodDate)){
                Toast.makeText(this, "Chưa đến hạn rút tiền!", Toast.LENGTH_SHORT).show();
            }
            else{

            }
        });

    }

    private void loadSavingInfor(String accountId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Accounts").document(accountId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Lấy dữ liệu từ document
                        Double balance = doc.getDouble("balance");
                        String maturityDate = doc.get("maturity_date") != null ? doc.get("maturity_date").toString() : "";
                        periodDate = doc.getDate("period_day");

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvPeriodDate.setText(sdf.format(periodDate));
                        if ("Không thời hạn".equals(maturityDate)) {
                            // Hiển thị ngày đáo hạn
                            tvMaturityDate.setText(maturityDate);
                            // Truy vấn InterestRates để lấy rate mới nhất
                            db.collection("InterestRates")
                                    .whereEqualTo("interest_type", "savings")
                                    .orderBy("created_at", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(querySnapshots -> {
                                        if (!querySnapshots.isEmpty()) {
                                            Double latestRate = querySnapshots.getDocuments().get(0).getDouble("interest_rate");
                                            tvSavingsRate.setText(latestRate + "% / năm");

                                            // Tính lợi nhuận tạm tính
                                            if (balance != null && latestRate != null) {
                                                estProfit = (balance * latestRate / 100) / 12;
                                                tvSavingsProfit.setText(String.format("+ %,.0f VND", estProfit));
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            tvSavingsRate.setText("Lỗi lấy lãi suất: " + e.getMessage()));
                        } else {
                            Date date = doc.getDate("maturity_date");
                            if (date != null) {
                                tvMaturityDate.setText(sdf.format(date));
                            }
                            // Nếu có kỳ hạn: lấy rate từ document
                            Double rate = doc.getDouble("interest_rate");
                            if (rate != null) {
                                tvSavingsRate.setText(rate + "% / năm");
                                if (balance != null) {
                                    estProfit = (balance * rate / 100) / 12;
                                    tvSavingsProfit.setText(String.format("+ %,.0f VND", estProfit));
                                }
                            }
                        }

                    }
                })
                .addOnFailureListener(e ->
                        tvSavingsRate.setText("Lỗi tải dữ liệu: " + e.getMessage()));
    }

}
