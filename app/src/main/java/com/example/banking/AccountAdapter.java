package com.example.banking;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.model.Account;

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
        holder.tvAccountName.setText(account.getAccount_type().equals("savings") ? "Tài khoản tiết kiệm" : "Tài khoản vay vốn");
        holder.tvAccountNumber.setText(account.getAccount_number());
        holder.tvAccountBalance.setText(String.format("%,.0f VND", account.getBalance()));

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            if(account.getAccount_type().equals("savings")) {
                Intent intent = new Intent(context, saving_infor.class);
                intent.putExtra("account_number", account.getAccount_number());
                context.startActivity(intent);
            }
            else{
                Intent intent = new Intent(context, mortgages_infor.class);
                intent.putExtra("account_number", account.getAccount_number());
                context.startActivity(intent);
            }
        });
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

