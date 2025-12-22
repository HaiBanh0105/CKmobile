package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.Activity.AccountTransactionActivity;
import com.example.banking.Activity.BaseSecureActivity;
import com.example.banking.databinding.ActivityBillPaymentBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.model.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class bill_payment extends BaseSecureActivity {

    private ActivityBillPaymentBinding binding;
    private FirebaseFirestore db;

    private Double totalAmount = 0.0;
    private String billCode, providerName;
    private String currentBillId;
    private String type;

    private final String userId = SessionManager.getInstance().getUserId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityBillPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initLoading(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        initProviderTabs();
        initEvents();
        type = getIntent().getStringExtra("BILL_TYPE");
        if (type != null && !type.isEmpty()) switchType(type);
    }

    private void initProviderTabs() {
        loadProviders("electric");
        binding.layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
        binding.layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));

        binding.cardElectric.setOnClickListener(v -> switchType("electric"));
        binding.cardWater.setOnClickListener(v -> switchType("water"));
    }

    private void switchType(String type) {
        binding.layoutElectric.setBackgroundColor(
                ContextCompat.getColor(this, type.equals("electric") ? R.color.highlight : R.color.grey));
        binding.layoutWater.setBackgroundColor(
                ContextCompat.getColor(this, type.equals("water") ? R.color.highlight : R.color.grey));

        loadProviders(type);
        binding.cardResult.setVisibility(View.GONE);
        binding.btnPay.setVisibility(View.GONE);
    }

    private void initEvents() {
        binding.btnLookup.setOnClickListener(v -> {
            providerName = binding.autoProvider.getText().toString().trim();
            billCode = binding.edtbillCode.getText().toString().trim();

            if (providerName.isEmpty() || billCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            loadBillInfo(billCode, providerName);
        });

        binding.btnPay.setOnClickListener(v -> {
            processPayment();
        });

        binding.autoProvider.setOnItemClickListener((parent, view, position, id) -> {
            binding.cardResult.setVisibility(View.GONE);
            binding.btnPay.setVisibility(View.GONE);
        });
    }

    private void processPayment() {
        showLoading(true);
        String transactionId = db.collection("AccountTransactions").document().getId();

        AccountTransaction transaction = new AccountTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setUserId(userId);
        transaction.setType("BILL");
        transaction.setAmount(totalAmount);
        transaction.setCategory("BILL_PAYMENT");
        transaction.setStatus("PENDING");
        transaction.setDescription("Thanh toán hóa đơn " + providerName + " - Mã: " + billCode);
        transaction.setReceiverName(providerName);
        transaction.setTimestamp(Timestamp.now());
        transaction.setBiometricRequired(false);

        db.runTransaction(firestoreTransaction -> {

            DocumentSnapshot billSnap = firestoreTransaction.get(
                    db.collection("billing").document(currentBillId)
            );

            if (!billSnap.exists()) throw new RuntimeException("Hóa đơn không tồn tại");

            if (!"UNPAID".equals(billSnap.getString("status")))
                throw new RuntimeException("Hóa đơn đã được thanh toán");

            // tạo transaction
            firestoreTransaction.set(
                    db.collection("AccountTransactions").document(transactionId),
                    transaction
            );

            // gắn transactionId vào billing
            firestoreTransaction.update(
                    db.collection("billing").document(currentBillId),
                    "transactionId", transactionId
            );

            return null;

        }).addOnSuccessListener(unused -> {
            showLoading(false);
            startActivity(new Intent(this, AccountTransactionActivity.class)
                    .putExtra("TRANSACTION_ID", transactionId));
            finish();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Thanh toán thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadProviders(String type) {
        db.collection("Payment_services").document(type).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> providers = (List<String>) documentSnapshot.get("providers");
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, providers);
                        binding.autoProvider.setAdapter(adapter);
                        binding.autoProvider.setText(providers.get(0), false);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải nhà cung cấp: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadBillInfo(String billCode, String providerName) {

        db.collection("billing")
                .whereEqualTo("billCode", billCode)
                .whereEqualTo("providerName", providerName)
                .whereEqualTo("status", "UNPAID")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot bill = querySnapshot.getDocuments().get(0);

                        currentBillId = bill.getId();
                        totalAmount = bill.getDouble("amount");

                        binding.tvCustomerName.setText("KH: " + bill.getString("customerName"));
                        binding.tvAddress.setText("ĐC: " + bill.getString("address"));
                        binding.tvTotalAmount.setText(String.format("%,.0f VND", totalAmount));

                        binding.cardResult.setVisibility(View.VISIBLE);
                        binding.btnPay.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Không tìm thấy hóa đơn", Toast.LENGTH_SHORT).show();
                        binding.cardResult.setVisibility(View.GONE);
                        binding.btnPay.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tra cứu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
