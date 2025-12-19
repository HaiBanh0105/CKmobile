package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class mortgages_infor extends AppCompatActivity {
    private String account_id;
    Double cheking_balance, monthlyPayment;

    private TextView tvMortgageRate, tvMortgagePrincipal, tvNextDueDate,  tvMonthlyPayment;

    MaterialButton btnPayMortgage;
    String userId= SessionManager.getInstance().getUserId();
    String email = SessionManager.getInstance().getEmail();
    private ListenerRegistration registration;
    private ActivityResultLauncher<Intent> Launcher;

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

        Launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                        FirestoreHelper helper = new FirestoreHelper();
                        helper.changeCheckingBalanceByUserId(this,userId,-monthlyPayment);
                        PayMortgage();
                        finish();
                    }
                }
        );

        Intent getIntent = getIntent();
        account_id = getIntent.getStringExtra("account_id");

        tvMortgageRate = findViewById(R.id.tvMortgageRate);
        tvMortgagePrincipal = findViewById(R.id.tvMortgagePrincipal);
        tvNextDueDate = findViewById(R.id.tvNextDueDate);
        tvMonthlyPayment = findViewById(R.id.tvMonthlyPayment);
        btnPayMortgage = findViewById(R.id.btnPayMortgage);

        loadCheckingInfor(userId);
        loadMortgageInfor(account_id);

        btnPayMortgage.setOnClickListener(v -> {
            if(monthlyPayment > cheking_balance){
                Toast.makeText(this, "Tài khoản thanh toaán không đủ: ", Toast.LENGTH_SHORT).show();
                return;
            }
            else{
                Intent intent = new Intent(mortgages_infor.this, otp.class);
                intent.putExtra("email", email);
                intent.putExtra("type", "transfer");
                intent.putExtra("amount", monthlyPayment.toString());
                Launcher.launch(intent);
            }
        });


    }

    private void loadMortgageInfor(String accountId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Accounts").document(accountId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Lấy dữ liệu từ document
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        Double remaining_debt = doc.getDouble("remaining_debt");
                        monthlyPayment = doc.getDouble("monthlyPayment");
                        Double interest_rate = doc.getDouble("interest_rate");
                        Date period_day = doc.getDate("period_day");

                        tvMortgageRate.setText(interest_rate + "% / năm");
                        tvMortgagePrincipal.setText(String.format("%,.0f VND", remaining_debt));
                        tvNextDueDate.setText(sdf.format(period_day));
                        tvMonthlyPayment.setText(String.format("%,.0f VND", monthlyPayment));

                        // Kiểm tra tháng hiện tại đã thanh toán chưa
                        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

                        db.collection("Payments")
                                .whereEqualTo("account_id", accountId)
                                .whereEqualTo("paid_month", currentMonth)
                                .get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        // Đã thanh toán tháng này
                                        btnPayMortgage.setEnabled(false);
                                        btnPayMortgage.setText("Đã thanh toán");
                                    } else {
                                        // Chưa thanh toán
                                        btnPayMortgage.setEnabled(true);
                                        btnPayMortgage.setText("Thanh toán ngay");
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi kiểm tra thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void PayMortgage(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Accounts").document(account_id).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double remaining_debt = doc.getDouble("remaining_debt");
                        Double monthlyPayment = doc.getDouble("monthlyPayment");
                        Date period_day = doc.getDate("period_day");

                        if (remaining_debt != null && monthlyPayment != null && period_day != null) {
                            // Trừ tiền thanh toán
                            double newRemainingDebt = remaining_debt - monthlyPayment;

                            // Cập nhật kỳ hạn tiếp theo (+1 tháng)
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(period_day);
                            cal.add(java.util.Calendar.MONTH, 1);
                            Date nextPeriod = cal.getTime();

                            // Cập nhật lại document Accounts
                            db.collection("Accounts").document(account_id)
                                    .update("remaining_debt", newRemainingDebt,
                                            "period_day", nextPeriod)
                                    .addOnSuccessListener(aVoid -> {
                                        // Lưu lịch sử thanh toán
                                        savePaymentHistory(account_id, monthlyPayment);

                                        // Cập nhật UI
                                        tvMortgagePrincipal.setText(String.format("%,.0f VND", newRemainingDebt));
                                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                        tvNextDueDate.setText(sdf.format(nextPeriod));

                                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
    }


    private void savePaymentHistory(String accountId, Double amountPaid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        java.util.Map<String, Object> payment = new java.util.HashMap<>();
        payment.put("account_id", accountId);
        payment.put("amount_paid", amountPaid);
        payment.put("paid_at", new Date());
        String paidMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        payment.put("paid_month", paidMonth);


        db.collection("Payments_history").add(payment)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this, "Đã lưu lịch sử thanh toán", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lưu lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        registration = helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                cheking_balance = balance;
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(mortgages_infor.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove(); // hủy listener khi Activity dừng
        }
    }

}