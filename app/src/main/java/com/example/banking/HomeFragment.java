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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvaccountNumber, tvbalance, tvWelcome;

    private ImageView btnToggleBalance, btnTransfer, btnTopUp, btnSavings;

    private boolean isBalanceVisible = false;

    private Double currentBalance; // lưu số dư hiện tại
    String userId = SessionManager.getInstance().getUserId();

    String name = SessionManager.getInstance().getUserName();
    String accountNumber;

    RecyclerView rvRecentTransactions;
    TransactionAdapter adapter;
    List<Transaction> transactionList = new ArrayList<>();

    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        tvaccountNumber = root.findViewById(R.id.tvAccountNumber);
        tvbalance = root.findViewById(R.id.tvBalanceAmount);
        tvWelcome = root.findViewById(R.id.tvWelcome);
        btnToggleBalance = root.findViewById(R.id.btnToggleBalance);
        btnTopUp = root.findViewById(R.id.btnTopUp);
        btnSavings = root.findViewById(R.id.btnSavings);
        btnTransfer = root.findViewById(R.id.btnTransfer);
        rvRecentTransactions = root.findViewById(R.id.rvRecentTransactions);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transactionList);
        rvRecentTransactions.setAdapter(adapter);

        loadCheckingInfor(userId);
        loadTransactions();

        tvWelcome.setText("Xin chào, "+ name);

        // Xử lý sự kiện click ẩn hiện số dư
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

        btnSavings.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), open_savings.class);
            startActivity(intent);
        });

        btnTopUp.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), top_up.class);
            startActivity(intent);
        });
        return root;
    }

    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        registration = helper.loadCheckingInfor(userId, new FirestoreHelper.AccountCallback() {
            @Override
            public void onSuccess(String number, Double balance){
                tvaccountNumber.setText("Số tài khoản: " + number);
                if (!isBalanceVisible) {
                    // Ẩn số dư
                    tvbalance.setText("********* VND");
                } else {
                    tvbalance.setText(String.format("%,.0f VND", balance));
                }
                currentBalance = balance;
                accountNumber = number;
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadTransactions() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Transactions")
                .whereEqualTo("user_id", userId)
                .orderBy("create_at", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(requireContext(), "Lỗi tải giao dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    transactionList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String name;

                        if ("received".equalsIgnoreCase(type)){
                            name = doc.getString("sender_name");
                        }
                        else{
                            name = doc.getString("receiver_name");
                        }

                        Date createdAt = doc.getDate("create_at");
                        String date = createdAt != null
                                ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(createdAt)
                                : "Đang xử lý...";
                        double amount = doc.getDouble("amount");
                        transactionList.add(new Transaction(name, date, amount,type));
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        loadCheckingInfor(userId);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove(); // hủy listener khi view bị hủy
        }
    }

}
