package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaos.view.PinView;
import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class otp extends AppCompatActivity {
    private PinView pinView;
    private MaterialButton btnConfirmOtp;
    private TextView tvResendOtp, tvTitle;
    private String currentOtp;

    String pin = SessionManager.getInstance().getPinNumber();

    String email, type;
    double amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.otp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.otp), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        email = intent.getStringExtra("email");

        pinView = findViewById(R.id.pinView);
        btnConfirmOtp = findViewById(R.id.btnConfirmOtp);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        tvTitle = findViewById(R.id.tvOtpInstruction);

        if(intent.hasExtra("type")){
            type = intent.getStringExtra("type");
            if ("transfer".equals(type)) {
                String amountStr = intent.getStringExtra("amount");
                amount = Double.parseDouble(amountStr);
                //Nhỏ hơn 2 triệu thì chỉ cần nhập mã pin
                if(amount < 2000000){
                    EnterPin();
                }
                else{
                    SendOtp();
                }
            }
            else if ("pin".equals(type)){
                EnterPin();
            }
        }
        else{
            SendOtp();
        }


        tvResendOtp.setOnClickListener(v -> {
            SendOtp();
        });

        btnConfirmOtp.setOnClickListener(v -> {
            String enteredOtp = pinView.getText().toString().trim();
                if("pin".equals(type)){
                    confirmPIN();
                }
                else if(amount < 2000000 && "transfer".equals(type)){
                    confirmPIN();
                }
                else{
                    confirmOTP();
                }

        });
    }

    private void EnterPin(){
        pinView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        tvTitle.setText("Nhập mã pin 6 số của ban");
        tvResendOtp.setVisibility(View.INVISIBLE);
    }

    private void confirmPIN(){
        String enteredPin = pinView.getText().toString().trim();
        if (enteredPin.equals(pin)) {
            Toast.makeText(otp.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("result_key", "OK");
            setResult(RESULT_OK, resultIntent);
            finish();
        }
        else {
            Toast.makeText(otp.this, "Mã pin không đúng!", Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmOTP(){
        String enteredOtp = pinView.getText().toString().trim();
        if (enteredOtp.equals(currentOtp)) {
            Toast.makeText(otp.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("result_key", "OK");
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(otp.this, "Mã OTP không đúng!", Toast.LENGTH_SHORT).show();
        }
    }



    private void SendOtp(){
        String otp = generateOtp();
        EmailService.sendEmail(this, email, "Mã OTP xác thực",
                "Mã OTP của bạn là: " + otp,
                new EmailService.EmailCallback() {
                    @Override
                    public void onSuccess() {
                        currentOtp = otp;
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(otp.this, "Không gửi được OTP: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }
}