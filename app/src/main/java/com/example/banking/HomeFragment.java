package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.banking.Activity.FlightTicketBooking;
import com.example.banking.Activity.MovieTicketBooking;
import com.example.banking.Activity.TransactionHistory;
import com.example.banking.Activity.transfer;
import com.example.banking.Adapter.TransactionAdapter;
import com.example.banking.databinding.FragmentHomeBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.util.ClickEffectUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private boolean isBalanceVisible = false;
    private Double currentBalance;

    private final String userId = SessionManager.getInstance().getUserId();
    private final String name = SessionManager.getInstance().getUserName();

    private TransactionAdapter adapter;
    private final List<AccountTransaction> transactionList = new ArrayList<>();

    private ListenerRegistration accountListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        setupUI();
        setupRecyclerView();
        setupEvents();

        loadCheckingInfor(userId);
        loadTransactions();

        return binding.getRoot();
    }

    private void setupUI() {
        binding.tvUserEmail.setText(SessionManager.getInstance().getEmail());
        binding.tvWelcome.setText("Xin chào, " + name);
        binding.tvBalanceAmount.setText("********* VND");
        String avatarUrl = SessionManager.getInstance().getAvatarUrl();

        if (!avatarUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.men)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .centerCrop()
                    .into(binding.imgAvatar);
        }
        ClickEffectUtil.apply(binding.btnTransfer);
        ClickEffectUtil.apply(binding.btnMovieTicket);
        ClickEffectUtil.apply(binding.btnFlightTicket);
        ClickEffectUtil.apply(binding.btnPayBill);
        ClickEffectUtil.apply(binding.btnTopUp);
        ClickEffectUtil.apply(binding.btnSavings);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transactionList, transaction -> {});
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(adapter);
    }

    private void setupEvents() {

        ClickEffectUtil.apply(binding.btnTransfer);

        // Ẩn / hiện số dư
        binding.btnToggleBalance.setOnClickListener(v -> {
            if (isBalanceVisible) {
                binding.tvBalanceAmount.setText("********* VND");
                binding.btnToggleBalance.setImageResource(R.drawable.ic_visibility_off);
            } else {
                if (currentBalance != null) {
                    binding.tvBalanceAmount.setText(
                            String.format("%,.0f VND", currentBalance)
                    );
                }
                binding.btnToggleBalance.setImageResource(R.drawable.ic_visibility);
            }
            isBalanceVisible = !isBalanceVisible;
        });

        binding.btnTransfer.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), transfer.class)));

        binding.btnSavings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), open_savings.class)));

        binding.btnTopUp.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), top_up.class)));

        binding.btnPayBill.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), bill_payment.class)));

        binding.tvViewMore.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TransactionHistory.class)));

        binding.btnFlightTicket.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FlightTicketBooking.class)));

        binding.btnMovieTicket.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MovieTicketBooking.class)));
    }

    // Load thông tin tài khoản
    private void loadCheckingInfor(String userId) {
        FirestoreHelper helper = new FirestoreHelper();
        accountListener = helper.loadCheckingInfor(userId,
                new FirestoreHelper.AccountCallback() {
                    @Override
                    public void onSuccess(String number, Double balance) {
                        if (binding != null) {
                            binding.tvAccountNumber.setText("Số tài khoản: " + number);
                            currentBalance = balance;

                            if (isBalanceVisible) {
                                binding.tvBalanceAmount.setText(
                                        String.format("%,.0f VND", balance)
                                );
                            }
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Load giao dịch từ AccountTransactions
    private void loadTransactions() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("AccountTransactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "SUCCESS")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || !isAdded()) return;

                    if (e != null) {
                        Toast.makeText(
                                requireContext(),
                                "Lỗi tải giao dịch: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    transactionList.clear();

                    if (snapshots == null) return;

                    for (DocumentSnapshot doc : snapshots) {

                        AccountTransaction transaction =
                                doc.toObject(AccountTransaction.class);

                        if (transaction == null) continue;

                        // Gán id nếu cần dùng về sau (detail)
                        transaction.setTransactionId(doc.getId());

                        transactionList.add(transaction);
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
        if (accountListener != null) {
            accountListener.remove();
        }
        binding = null; // tránh leak memory
    }
}
