package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class bill_payment extends AppCompatActivity {

    private TextView tvTotalAmount;
    private LinearLayout layoutElectric, layoutWater;
    Double TotalAmount;
    TextInputEditText edtbillCode;
    Button btnLookup,btnPay;
    AutoCompleteTextView autoProvider;

    private ActivityResultLauncher<Intent> confirmLauncher;

    String userId = SessionManager.getInstance().getUserId();
    String billCode, providerName;

    CardView cardResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bill_payment);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bill_payment), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        CardView cardElectric = findViewById(R.id.cardElectric);
        CardView cardWater = findViewById(R.id.cardWater);

        cardResult = findViewById(R.id.cardResult);
        layoutElectric = cardElectric.findViewById(R.id.layoutElectric);
        layoutWater = cardWater.findViewById(R.id.layoutWater);
        btnLookup = findViewById(R.id.btnLookup);
        btnPay = findViewById(R.id.btnPay);
        autoProvider = findViewById(R.id.autoProvider);
        edtbillCode = findViewById(R.id.edtbillCode);

        loadProviders("electric");
        layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
        layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));

        confirmLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){

                                FirestoreHelper helper = new FirestoreHelper();
                                helper.changeCheckingBalanceByUserId(this,userId,-TotalAmount);
                                updateBillStatus(billCode,providerName,userId );
                                finish();
                            }
                            else{
                                Toast.makeText(this, "Thanh toán thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        cardElectric.setOnClickListener(v -> {
            layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
            layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            loadProviders("electric");
            cardResult.setVisibility(View.GONE);
            btnPay.setVisibility(View.GONE);
        });

        cardWater.setOnClickListener(v -> {
            layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
            layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            loadProviders("water");
            cardResult.setVisibility(View.GONE);
            btnPay.setVisibility(View.GONE);
        });


        btnLookup.setOnClickListener(v -> {
            providerName = autoProvider.getText().toString().trim();
            billCode = edtbillCode.getText().toString().trim();

            if (providerName.isEmpty() || billCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loadBillInfo(billCode, providerName);
            }
        });

        btnPay.setOnClickListener(v -> {
            String amount = String.format("%,.0f", TotalAmount);
            Intent intent = new Intent(bill_payment.this, otp.class);
            intent.putExtra("type", "transfer");
            intent.putExtra("amount", amount);
            confirmLauncher.launch(intent);
        });

        autoProvider.setOnItemClickListener((parent, view, position, id) -> {
            cardResult.setVisibility(View.GONE);
            btnPay.setVisibility(View.GONE);
        });

    }

    private void loadProviders(String type) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AutoCompleteTextView autoProvider = findViewById(R.id.autoProvider);

        db.collection("Payment_services").document(type).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> providers = (List<String>) documentSnapshot.get("providers");
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, providers);
                        autoProvider.setAdapter(adapter);
                        autoProvider.setText(providers.get(0), false);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải nhà cung cấp: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadBillInfo(String billCode, String providerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvAddress = findViewById(R.id.tvAddress);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        db.collection("billing")
                .whereEqualTo("billCode", billCode)
                .whereEqualTo("providerName", providerName)
                .whereEqualTo("status", "UNPAID")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // lấy document đầu tiên
                        DocumentSnapshot bill = querySnapshot.getDocuments().get(0);

                        String customerName = bill.getString("customerName");
                        String address = bill.getString("address");
                        TotalAmount = bill.getDouble("amount");

                        tvCustomerName.setText("KH: " + customerName);
                        tvAddress.setText("ĐC: " + address);
                        tvTotalAmount.setText(String.format("%,.0f VND", TotalAmount));

                        cardResult.setVisibility(View.VISIBLE);
                        btnPay.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Không tìm thấy hóa đơn", Toast.LENGTH_SHORT).show();
                        cardResult.setVisibility(View.GONE);
                        btnPay.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tra cứu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateBillStatus(String billCode, String providerName, String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("billing")
                .whereEqualTo("billCode", billCode)
                .whereEqualTo("providerName", providerName)
                .whereEqualTo("status", "UNPAID") // chỉ cập nhật hóa đơn chưa thanh toán
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot bill = querySnapshot.getDocuments().get(0);

                        // Lấy reference của document
                        String docId = bill.getId();
                        db.collection("billing").document(docId)
                                .update(
                                        "status", "PAID",
                                        "paidBy", userID,
                                        "paidAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Không tìm thấy hóa đơn để cập nhật", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tra cứu hóa đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}