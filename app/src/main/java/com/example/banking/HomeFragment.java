package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView tvaccountNumber, tvbalance;

    private ImageView btnToggleBalance, btnTransfer;

    private boolean isBalanceVisible = false;

    private Double currentBalance; // lưu số dư hiện tại
    String userId = SessionManager.getInstance().getUserId();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        tvaccountNumber = root.findViewById(R.id.tvAccountNumber);
        tvbalance = root.findViewById(R.id.tvBalanceAmount);
        btnToggleBalance = root.findViewById(R.id.btnToggleBalance);
        btnTransfer = root.findViewById(R.id.btnTransfer);

        loadCheckingInfor(userId);

        // Xử lý sự kiện click
        btnToggleBalance.setOnClickListener(v -> {
            if (isBalanceVisible) {
                // Ẩn số dư
                tvbalance.setText("********* VND");
                btnToggleBalance.setImageResource(R.drawable.ic_visibility_off); // icon hiện
            } else {
                // Hiện số dư
                if (currentBalance != null) {
                    tvbalance.setText(String.format("%,.0f VND", currentBalance));
                }
                btnToggleBalance.setImageResource(R.drawable.ic_visibility); // icon ẩn
            }
            isBalanceVisible = !isBalanceVisible;
        });

        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), transfer.class);
            startActivity(intent);
        });

        return root;
    }

    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                tvaccountNumber.setText("Số tài khoản: " + number);
                tvbalance.setText("********* VND");
                currentBalance = balance;
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
