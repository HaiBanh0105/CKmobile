package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class staff_main extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.staff_main);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.staff_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_staff_main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        drawerLayout = findViewById(R.id.activity_staff_main);
        navView = findViewById(R.id.nav_view);
        View headerView = navView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.staffName);
        toolbar = findViewById(R.id.toolbar);

        String userName = SessionManager.getInstance().getUserName();
        tvName.setText("Nhân viên: "+ userName);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new staff_home())
                        .commit();
            } else if (id == R.id.nav_manage_interest) {
                startActivity(new Intent(this, interest_rate.class));
            } else if (id == R.id.nav_logout) {
                startActivity(new Intent(this, login.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load mặc định Dashboard khi mở app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new staff_home())
                    .commit();
            navView.setCheckedItem(R.id.nav_dashboard);

        }
    }
}