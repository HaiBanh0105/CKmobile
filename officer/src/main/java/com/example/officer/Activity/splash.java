package com.example.officer.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.officer.R;

public class splash extends AppCompatActivity {
    private static final int SPLASH_DISPLAY_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cách 1: Dùng View Binding (khuyến nghị)
        // ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        // setContentView(binding.getRoot());

        // Cách 2: Dùng setContentView truyền thống
        setContentView(R.layout.splash);

        // Sử dụng Handler để trì hoãn việc chuyển Activity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tạo một Intent để bắt đầu LoginActivity
                Intent mainIntent = new Intent(splash.this, login.class);
                splash.this.startActivity(mainIntent);

                // Đóng SplashActivity để người dùng không thể quay lại nó
                splash.this.finish();
            }
        }, SPLASH_DISPLAY_DURATION);
    }
}