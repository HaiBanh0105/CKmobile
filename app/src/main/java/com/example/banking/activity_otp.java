package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
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

public class activity_otp extends AppCompatActivity {
    private PinView pinView;
    private MaterialButton btnConfirmOtp;
    private TextView tvResendOtp;
    private String currentOtp;

    String email;

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

        SendOtp();
        tvResendOtp.setOnClickListener(v -> {
            SendOtp();
        });

        btnConfirmOtp.setOnClickListener(v -> {
            String enteredOtp = pinView.getText().toString().trim();
            if (enteredOtp.equals(currentOtp)) {
                Toast.makeText(activity_otp.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result_key", "OK");
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(activity_otp.this, "Mã OTP không đúng!", Toast.LENGTH_SHORT).show();
            }
        });
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
                        Toast.makeText(activity_otp.this, "Không gửi được OTP: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }
}