package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class saving_infor extends AppCompatActivity {
    private TextView tvSavingsRate, tvSavingsProfit, tvMaturityDate, tvPeriodDate;
    double estProfit;
    String account_Id;
    Date periodDate;
    Button btnWithdraw;
    String userId= SessionManager.getInstance().getUserId();
    String email = SessionManager.getInstance().getEmail();
    private ActivityResultLauncher<Intent> Launcher;

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

        Launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        performWithdrawal();
                        finish();
                    }
                }
        );

        tvSavingsRate = findViewById(R.id.tvSavingsRate);
        tvSavingsProfit = findViewById(R.id.tvSavingsProfit);
        tvMaturityDate = findViewById(R.id.tvMaturityDate);
        tvPeriodDate = findViewById(R.id.tvPeriodDate);
        btnWithdraw = findViewById(R.id.btnWithdraw);


        // Nhận dữ liệu từ Intent
        Intent getIntent = getIntent();
        account_Id = getIntent.getStringExtra("account_id");

        loadSavingInfor(account_Id);

        btnWithdraw.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();

            if(today.before(periodDate)){
                Toast.makeText(this, "Chưa đến hạn rút tiền!", Toast.LENGTH_SHORT).show();
                return;
            }
            else{
                Intent intent = new Intent(saving_infor.this, otp.class);
                intent.putExtra("email", email);
                intent.putExtra("type", "transfer");
                intent.putExtra("amount", String.valueOf(estProfit));
                Launcher.launch(intent);
            }
        });

    }

    private void performWithdrawal() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //Cộng lãi vào tài khoản thanh toán
        FirestoreHelper helper = new FirestoreHelper();
        helper.changeCheckingBalanceByUserId(this, userId, estProfit);

        // Cập nhật ngày kỳ hạn tiếp theo
        Calendar next = Calendar.getInstance();
        next.setTime(periodDate);
        next.add(Calendar.MONTH, 1);
        Date nextPeriod = next.getTime();

        db.collection("Accounts").document(account_Id)
                .update("period_day", nextPeriod)
                .addOnSuccessListener(aVoid -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    tvPeriodDate.setText(sdf.format(nextPeriod));
                });


        // 4. Thông báo và cập nhật UI
        Toast.makeText(this, "Rút tiền thành công!", Toast.LENGTH_LONG).show();
        btnWithdraw.setEnabled(false);
        btnWithdraw.setText("Đã rút tháng này");
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

        // Sau khi hiển thị thông tin tài khoản
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        db.collection("Transactions")
                .whereEqualTo("account_id", accountId)
                .whereEqualTo("transaction_month", currentMonth)
                .whereEqualTo("type", "withdraw_savings")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        // Đã rút trong tháng này
                        btnWithdraw.setEnabled(false);
                        btnWithdraw.setText("Đã rút tháng này");
                    } else {
                        // Chưa rút
                        btnWithdraw.setEnabled(true);
                        btnWithdraw.setText("Rút tiền");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kiểm tra lịch sử rút: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void saveWithdrawalRecord(String accountId, double amount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        Map<String, Object> record = new HashMap<>();
        record.put("account_id", accountId);
        record.put("user_id", userId);
        record.put("type", "withdraw_savings");
        record.put("amount", amount);
        record.put("transaction_month", month);
        record.put("transaction_date", new Date());
        record.put("note", "Rút lãi tiết kiệm tháng "+ month);

        db.collection("Transactions").add(record);
    }


}
