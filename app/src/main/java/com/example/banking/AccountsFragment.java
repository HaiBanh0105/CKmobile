package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.model.Account;
import com.example.banking.model.AccountItem;
import com.example.banking.model.MortgageAccount;
import com.example.banking.model.SavingsAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {
    private RecyclerView rvAccounts;
    private AccountAdapter adapter;
    private List<AccountItem> accountItemList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId = SessionManager.getInstance().getUserId();
    private ProgressBar progressBar;

    FloatingActionButton openSaving;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts, container, false);

        openSaving = root.findViewById(R.id.fabOpenSavings);
        rvAccounts = root.findViewById(R.id.rvAccounts);
        progressBar = root.findViewById(R.id.progressBar);
        rvAccounts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AccountAdapter(accountItemList);
        rvAccounts.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadAccounts();

        openSaving.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), open_savings.class);
            startActivity(intent);
        });

        return  root;
    }

    private void loadAccounts() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(query -> {

                    accountItemList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {

                        String type = doc.getString("account_type");

                        if ("SAVINGS".equalsIgnoreCase(type)) {

                            SavingsAccount savings =
                                    doc.toObject(SavingsAccount.class);
                            accountItemList.add(new AccountItem(savings));

                        } else if ("MORTGAGE".equalsIgnoreCase(type)) {

                            MortgageAccount mortgage =
                                    doc.toObject(MortgageAccount.class);
                            accountItemList.add(new AccountItem(mortgage));
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(
                            getContext(),
                            "Lỗi tải dữ liệu: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAccounts();
    }
}
