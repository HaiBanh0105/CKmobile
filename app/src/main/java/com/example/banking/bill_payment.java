package com.example.banking;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
    private ImageView imgElectric, imgWater;
    private TextView tvElectric, tvWater;
    private LinearLayout layoutElectric, layoutWater;

    TextInputEditText edtCustomerCode;
    Button btnLookup;
    AutoCompleteTextView autoProvider;

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

        layoutElectric = cardElectric.findViewById(R.id.layoutElectric);
        layoutWater = cardWater.findViewById(R.id.layoutWater);
        btnLookup = findViewById(R.id.btnLookup);
        autoProvider = findViewById(R.id.autoProvider);
        edtCustomerCode = findViewById(R.id.edtCustomerCode);

        cardElectric.setOnClickListener(v -> {
            layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
            layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            loadProviders("electric");
        });

        cardWater.setOnClickListener(v -> {
            layoutWater.setBackgroundColor(ContextCompat.getColor(this, R.color.highlight));
            layoutElectric.setBackgroundColor(ContextCompat.getColor(this, R.color.grey));
            loadProviders("water");
        });


        btnLookup.setOnClickListener(v -> {
            String providerName = autoProvider.getText().toString().trim();
            String customerCode = edtCustomerCode.getText().toString().trim();

            if (providerName.isEmpty() || customerCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loadBillInfo(customerCode, providerName);
            }
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

    private void loadBillInfo(String customerCode, String providerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        CardView cardResult = findViewById(R.id.cardResult);

        db.collection("billing")
                .whereEqualTo("customerCode", customerCode)
                .whereEqualTo("providerName", providerName)
                .whereEqualTo("status", "UNPAID") // chỉ lấy hóa đơn chưa thanh toán
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // lấy document đầu tiên
                        DocumentSnapshot bill = querySnapshot.getDocuments().get(0);

                        String customerName = bill.getString("customerName");
                        String address = bill.getString("address");
                        Long amount = bill.getLong("amount");

                        tvCustomerName.setText("KH: " + customerName);
                        tvAddress.setText("ĐC: " + address);
                        tvTotalAmount.setText(String.format("%,d VND", amount));

                        cardResult.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Không tìm thấy hóa đơn", Toast.LENGTH_SHORT).show();
                        cardResult.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tra cứu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



}