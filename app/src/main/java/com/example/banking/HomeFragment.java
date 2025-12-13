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
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvaccountNumber, tvbalance;

    private ImageView btnToggleBalance, btnTransfer;

    private boolean isBalanceVisible = false;

    private Double currentBalance; // lưu số dư hiện tại
    String userId = SessionManager.getInstance().getUserId();

    String accountNumber;

    RecyclerView rvRecentTransactions;
    TransactionAdapter adapter;
    List<Transaction> transactionList = new ArrayList<>();

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
        rvRecentTransactions = root.findViewById(R.id.rvRecentTransactions);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transactionList);
        rvRecentTransactions.setAdapter(adapter);

        loadCheckingInfor(userId);
        loadTransactions();

        // Xử lý sự kiện click ẩn hiện số dư
        btnToggleBalance.setOnClickListener(v -> {

//            // Đăng ký listener realtime sau khi đã gán tvbalance
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            db.collection("Accounts")
//                    .document(accountNumber)
//                    .addSnapshotListener((snapshot, e) -> {
//                        if (snapshot != null && snapshot.exists()) {
//                            Double newBalance = snapshot.getDouble("balance");
//                            currentBalance = newBalance;
//                        }
//                    });

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
                isBalanceVisible = false;
                currentBalance = balance;
                accountNumber = number;

                // Đăng ký listener realtime balance
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Accounts")
                        .document(accountNumber)
                        .addSnapshotListener((snapshot, e) -> {
                            if (snapshot != null && snapshot.exists()) {
                                Double newBalance = snapshot.getDouble("balance");
                                currentBalance = newBalance;
                                if (isBalanceVisible) {
                                    tvbalance.setText(String.format("%,.0f VND", newBalance));
                                }
                            }
                        });
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
                .whereEqualTo("sender_id", userId)
                .orderBy("create_at", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(requireContext(), "Lỗi tải giao dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    transactionList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("receiver_name");
                        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(doc.getDate("create_at"));
                        double amount = doc.getDouble("amount");
                        transactionList.add(new Transaction(name, date, amount));
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        loadCheckingInfor(userId);

    }

}
