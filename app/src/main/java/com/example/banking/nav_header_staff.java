package com.example.banking;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class nav_header_staff extends AppCompatActivity {
    String userName = SessionManager.getInstance().getUserName();
    TextView staffName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.nav_header_staff);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_header_staff), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        staffName = findViewById(R.id.staffName);
        staffName.setText("Nhân viên: "+ userName);
    }

}