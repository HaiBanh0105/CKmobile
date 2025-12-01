package com.example.banking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {
    private List<Account> accountList;

    public AccountAdapter(List<Account> accountList) {
        this.accountList = accountList;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accountList.get(position);
        holder.tvAccountName.setText(account.getAccount_type().equals("checking") ? "Tài khoản thanh toán" : "Tài khoản tiết kiệm");
        holder.tvAccountNumber.setText(account.getAccount_id());
        holder.tvAccountBalance.setText(String.format("%,.0f VND", account.getBalance()));
    }

    @Override
    public int getItemCount() {
        return accountList.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvAccountNumber, tvAccountBalance;
        ImageView imgAccountIcon;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountName = itemView.findViewById(R.id.tvAccountName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvAccountBalance = itemView.findViewById(R.id.tvAccountBalance);
            imgAccountIcon = itemView.findViewById(R.id.imgAccountIcon);
        }
    }
}

