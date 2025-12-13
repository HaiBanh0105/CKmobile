package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class transfer extends AppCompatActivity {
    private TextView tvcheckingAmount;

    MaterialButton btnContinue;
    TextInputEditText edtAccountNumber,edtAccountName,edtAmount,edtContent;
    private MaterialToolbar toolbar;
    String userId = SessionManager.getInstance().getUserId();

    String receiverId;
    String userName = SessionManager.getInstance().getUserName();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ActivityResultLauncher<Intent> confirmLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.transfer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transfer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Đăng ký nhận kết quả
        confirmLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        boolean confirmed = result.getData().getBooleanExtra("confirmed", false);
                        if (confirmed) {
                            Toast.makeText(this, "Giao dịch đã xác nhận!", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("transactionSuccess", true);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Toast.makeText(this, "Giao dịch bị hủy!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


        toolbar = findViewById(R.id.toolbar);
        edtAccountNumber = findViewById(R.id.edtAccountNumber);
        edtAccountName = findViewById(R.id.edtAccountName);
        tvcheckingAmount = findViewById(R.id.tvSourceBalance);
        edtAmount = findViewById(R.id.edtAmount);
        edtContent = findViewById(R.id.edtContent);
        btnContinue = findViewById(R.id.btnContinue);

        loadCheckingInfor(userId);

        edtContent.setText(userName + " chuyển tiền");


        edtAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String accountNumber = s.toString().trim();


                if (accountNumber.length() != 12) {
                    edtAccountName.setText("Không tìm thấy");
                }else{
                    loadAccountName(accountNumber);
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            String accountNumber = edtAccountNumber.getText() != null ? edtAccountNumber.getText().toString().trim() : "";
            String accountName   = edtAccountName.getText() != null ? edtAccountName.getText().toString().trim() : "";
            String amount        = edtAmount.getText() != null ? edtAmount.getText().toString().trim() : "";
            String content   = edtContent.getText() != null ? edtContent.getText().toString().trim() : "";

            if (TextUtils.isEmpty(accountNumber)) {
                Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(accountName) || accountName.equalsIgnoreCase("Không tìm thấy")) {
                Toast.makeText(this, "Tài khoản không tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "Vui lòng nhập nội dung chuyển khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(transfer.this,transfer_confirm.class);
            intent.putExtra("accountNumber",accountNumber);
            intent.putExtra("accountName",accountName);
            intent.putExtra("amount",amount);
            intent.putExtra("content",content);
            intent.putExtra("receiverId",receiverId);
            confirmLauncher.launch(intent);

        });


        //Nút trở về
        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    //Cập nhật tiền thanh toán giao diện
    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                tvcheckingAmount.setText(String.format("%,.0f VND", balance));
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(transfer.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Tải tên người dùng khi nhập số tài khoản
    private void loadAccountName(String accountNumber) {

        db.collection("Accounts")
                .whereEqualTo("account_type", "checking")
                .whereEqualTo("account_number", accountNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = doc.getString("name");
                        receiverId = doc.getString("user_id");
                        edtAccountName.setText(name != null ? name : "Không rõ");
                    } else {
                        edtAccountName.setText("Không tìm thấy");

                    }
                })
                .addOnFailureListener(e -> {
                    edtAccountName.setText("Lỗi kết nối");
                });
    }

}