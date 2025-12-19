package com.example.banking;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class top_up extends AppCompatActivity {
    private TextInputEditText edtPhone;
    ImageView btnPickContact;
    private TextView tvCarrier,tvBalance;
    private Button btnContinue;

    // Danh sách các nút mệnh giá
    private List<AppCompatButton> amountButtons = new ArrayList<>();

    String phone;
    String userId = SessionManager.getInstance().getUserId();

    String User_phone = SessionManager.getInstance().getPhone();


    String email = SessionManager.getInstance().getEmail();
    double currentBalance, selectedAmount = 0;
    private ActivityResultLauncher<Intent> launcher;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_top_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.top_up), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtPhone = findViewById(R.id.edtPhoneNumber);
        tvCarrier = findViewById(R.id.tvCarrier);
        btnContinue = findViewById(R.id.btnContinue);
        tvBalance = findViewById(R.id.tvBalance);
        btnPickContact = findViewById(R.id.btnPickContact);

        loadCheckingInfor(userId);

        setupAmountButtons();
        setupPhoneInputListener();

        // Khởi tạo launcher
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String value = data.getStringExtra("result_key");
                            if(value.equalsIgnoreCase("OK")){

                                FirestoreHelper helper = new FirestoreHelper();
                                helper.changeCheckingBalanceByUserId(this,userId,-selectedAmount);

                                Toast.makeText(this, "Nạp thành công " + selectedAmount + "đ cho " + phone, Toast.LENGTH_SHORT).show();
                                edtPhone.setText("");

                                for (AppCompatButton btn : amountButtons) {
                                    btn.setSelected(false);
                                }
                                selectedAmount = 0;
                                loadCheckingInfor(userId);

                            }
                        }
                    }
                }
        );

        btnContinue.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString();
            if(phone.isEmpty()){
                edtPhone.setText(User_phone);
                return;
            }
            else if (phone.length() < 9) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedAmount == 0) {
                Toast.makeText(this, "Vui lòng chọn mệnh giá", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentBalance < selectedAmount) {
                Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
                return;
            }


            Intent intent = new Intent(top_up.this, otp.class);
            intent.putExtra("email",email);
            intent.putExtra("type","pin");
            launcher.launch(intent);

        });

        btnPickContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, 200);
        });
    }

    private void setupAmountButtons() {
        amountButtons.add(findViewById(R.id.btn10k));
        amountButtons.add(findViewById(R.id.btn20k));
        amountButtons.add(findViewById(R.id.btn50k));
        amountButtons.add(findViewById(R.id.btn100k));
        amountButtons.add(findViewById(R.id.btn200k));
        amountButtons.add(findViewById(R.id.btn500k));

        View.OnClickListener listener = view -> {
            AppCompatButton clickedBtn = (AppCompatButton) view;
            for (AppCompatButton btn : amountButtons) {
                btn.setSelected(false);
            }
            clickedBtn.setSelected(true);
            selectedAmount = Double.parseDouble(clickedBtn.getTag().toString());
        };

        for (AppCompatButton btn : amountButtons) {
            btn.setOnClickListener(listener);
        }
    }

    private void setupPhoneInputListener() {
        edtPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phone = s.toString();
                if (phone.length() >= 3) {
                    String carrier = detectCarrier(phone);
                    if (!carrier.isEmpty()) {
                        tvCarrier.setVisibility(View.VISIBLE);
                        tvCarrier.setText(carrier);
                    } else {
                        tvCarrier.setVisibility(View.GONE);
                    }
                } else {
                    tvCarrier.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Hàm nhận diện nhà mạng
    private String detectCarrier(String phone) {
        if (phone.startsWith("03") || phone.startsWith("086") || phone.startsWith("096") || phone.startsWith("097") || phone.startsWith("098")) return "Viettel";
        if (phone.startsWith("07") || phone.startsWith("089") || phone.startsWith("090") || phone.startsWith("093")) return "MobiFone";
        if (phone.startsWith("08") || phone.startsWith("094") || phone.startsWith("091")) return "VinaPhone";
        if (phone.startsWith("05") || phone.startsWith("092")) return "Vietnamobile";
        return "";
    }

    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        registration = helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                tvBalance.setText(String.format("%,.0f VND", balance));
                currentBalance = balance;
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(top_up.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(numberIndex);
                cursor.close();

                edtPhone.setText(phoneNumber);
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove(); // hủy listener khi Activity dừng
        }
    }
}