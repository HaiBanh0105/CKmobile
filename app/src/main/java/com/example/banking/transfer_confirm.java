package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class transfer_confirm extends AppCompatActivity {
    private TextView tvConfirmAmount, tvConfirmAccountNumber, tvConfirmAccountName, tvConfirmContent;
    private MaterialButton btnConfirm;
    private MaterialToolbar toolbar;

    String receiverId;

    String email = SessionManager.getInstance().getEmail();

    String userId = SessionManager.getInstance().getUserId();

    String userName = SessionManager.getInstance().getUserName();

    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.transfer_confirm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transfer_confirm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy dữ liệu từ Intent
        Intent getIntent = getIntent();
        String accountNumber = getIntent.getStringExtra("accountNumber");
        String accountName   = getIntent.getStringExtra("accountName");
        String amount        = getIntent.getStringExtra("amount");
        String content       = getIntent.getStringExtra("content");
        receiverId = getIntent.getStringExtra("receiverId");

        // Khởi tạo launcher
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){


                                double amountDb = Double.parseDouble(amount);
                                FirestoreHelper helper = new FirestoreHelper();
                                helper.changeCheckingBalanceByUserId(this,userId,-amountDb);
                                helper.changeCheckingBalanceByUserId(this,receiverId,amountDb);

                                //Lưu lịch sử giao dịch
                                saveTransactionHistory(userId, receiverId, "sent" ,accountNumber, accountName, amountDb, content);
                                saveTransactionHistory(userId, receiverId, "received" ,accountNumber, userName , amountDb, content);

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("confirmed", true);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            }
                        }
                    }
                }
        );

        tvConfirmAmount = findViewById(R.id.tvConfirmAmount);
        tvConfirmAccountNumber = findViewById(R.id.tvConfirmAccountNumber);
        tvConfirmAccountName = findViewById(R.id.tvConfirmAccountName);
        tvConfirmContent = findViewById(R.id.tvConfirmContent);
        btnConfirm = findViewById(R.id.btnConfirm);
        toolbar = findViewById(R.id.toolbar);


        // Gán dữ liệu lên UI
        tvConfirmAmount.setText(amount + " VND");
        tvConfirmAccountNumber.setText(accountNumber);
        tvConfirmAccountName.setText(accountName);
        tvConfirmContent.setText(content);

        // Xử lý nút xác nhận
        btnConfirm.setOnClickListener(v -> {
            Intent intent = new Intent(transfer_confirm.this, otp.class);
            intent.putExtra("email",email);
            intent.putExtra("type","transfer");
            intent.putExtra("amount",amount);
            launcher.launch(intent);
        });


        //Nút trở về
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    //Lưu lịch sử giao dịch
    private void saveTransactionHistory(String senderId, String receiverId, String type,
                                        String accountNumber, String accountName,
                                        double amount, String content) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> transaction = new HashMap<>();

        // Sửa key đúng
        transaction.put("type", type);

        if ("sent".equalsIgnoreCase(type)) {
            transaction.put("user_id", senderId);
            transaction.put("receiver_id", receiverId);
            transaction.put("receiver_account_number", accountNumber);
            transaction.put("receiver_name", accountName);
        } else if ("received".equalsIgnoreCase(type)) {
            transaction.put("user_id", receiverId);
            transaction.put("sender_id", senderId);
            transaction.put("sender_account_number", accountNumber);
            transaction.put("sender_name", accountName);
        }

        transaction.put("amount", amount);
        transaction.put("content", content);
        transaction.put("create_at", FieldValue.serverTimestamp());

        db.collection("Transactions").add(transaction);
    }

}