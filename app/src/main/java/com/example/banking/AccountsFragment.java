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
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.model.Account;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {
    private RecyclerView rvAccounts;
    private AccountAdapter adapter;
    private List<Account> accountList = new ArrayList<>();
    private FirebaseFirestore db;
    private String userId = SessionManager.getInstance().getUserId();

    FloatingActionButton openSaving;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_accounts, container, false);

        openSaving = root.findViewById(R.id.fabOpenSavings);
        rvAccounts = root.findViewById(R.id.rvAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AccountAdapter(accountList);
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
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accountList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Account account = doc.toObject(Account.class);
                        if (!"checking".equals(account.getAccount_type())) {
                            accountList.add(account);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAccounts();
    }
}
