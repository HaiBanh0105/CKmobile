package com.example.baking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_splash extends AppCompatActivity {
    private static final int SPLASH_DISPLAY_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cách 1: Dùng View Binding (khuyến nghị)
        // ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        // setContentView(binding.getRoot());

        // Cách 2: Dùng setContentView truyền thống
        setContentView(R.layout.activity_splash);

        // Sử dụng Handler để trì hoãn việc chuyển Activity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tạo một Intent để bắt đầu LoginActivity
                Intent mainIntent = new Intent(activity_splash.this, activity_login.class);
                activity_splash.this.startActivity(mainIntent);

                // Đóng SplashActivity để người dùng không thể quay lại nó
                activity_splash.this.finish();
            }
        }, SPLASH_DISPLAY_DURATION);
    }
}