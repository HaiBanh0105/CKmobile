package com.example.banking;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class transfer extends AppCompatActivity {
    private TextView tvcheckingAmount;

    TextInputEditText edtAccountNumber,edtAccountName,edtAmount,edtContent;
    private MaterialToolbar toolbar;
    String userId = SessionManager.getInstance().getUserId();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        toolbar = findViewById(R.id.toolbar);
        edtAccountNumber = findViewById(R.id.edtAccountNumber);
        edtAccountName = findViewById(R.id.edtAccountName);
        tvcheckingAmount = findViewById(R.id.tvSourceBalance);
        edtAmount = findViewById(R.id.edtAmount);
        edtContent = findViewById(R.id.edtContent);

        loadCheckingInfor(userId);

        // Theo dõi khi người dùng nhập xong số tài khoản
//        edtAccountNumber.setOnFocusChangeListener((v, hasFocus) -> {
//            if (!hasFocus) { // khi người dùng rời khỏi ô nhập
//                String accountNumber = edtAccountNumber.getText().toString().trim();
//                if (accountNumber.length() == 12) { // kiểm tra độ dài hợp lệ
//                    loadAccountName(accountNumber);
//                }
//            }
//        });

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

                // Chỉ gọi Firestore khi nhập đủ 12 số
                if (accountNumber.length() != 12) {
                    edtAccountName.setText("Không tìm thấy");
                    edtContent.setText("");
                }else{
                    loadAccountName(accountNumber);
                }
            }
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
                        edtAccountName.setText(name != null ? name : "Không rõ");
                        edtContent.setText(name + " chuyển tiền");
                    } else {
                        edtAccountName.setText("Không tìm thấy");
                        edtContent.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    edtAccountName.setText("Lỗi kết nối");
                });
    }

}