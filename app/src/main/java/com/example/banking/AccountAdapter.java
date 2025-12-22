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
import com.example.banking.model.AccountItem;
import com.example.banking.model.MortgageAccount;
import com.example.banking.model.SavingsAccount;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {
    private final List<AccountItem> items;

    public AccountAdapter(List<AccountItem> items) {
        this.items = items;
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
        AccountItem item = items.get(position);

        if (item.getType() == AccountItem.TYPE_SAVINGS) {

            SavingsAccount acc = item.getSavings();
            holder.tvAccountName.setText("Tài khoản tiết kiệm");
            holder.tvAccountBalance.setText(
                    String.format("%,.0f VND", acc.getBalance())
            );

            holder.itemView.setOnClickListener(v -> {
                Context c = v.getContext();
                Intent i = new Intent(c, saving_infor.class);
                i.putExtra("account_number", acc.getAccount_number());
                c.startActivity(i);
            });

        } else {

            MortgageAccount acc = item.getMortgage();
            holder.tvAccountName.setText("Tài khoản vay vốn");
            holder.tvAccountBalance.setText(
                    String.format("Dư nợ: %,.0f VND", acc.getRemaining_debt())
            );

            holder.itemView.setOnClickListener(v -> {
                Context c = v.getContext();
                Intent i = new Intent(c, mortgages_infor.class);
                i.putExtra("account_number", acc.getAccount_number());
                c.startActivity(i);
            });
        }

        holder.tvAccountNumber.setText(
                item.getType() == AccountItem.TYPE_SAVINGS
                        ? item.getSavings().getAccount_number()
                        : item.getMortgage().getAccount_number()
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvAccountNumber, tvAccountBalance;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountName = itemView.findViewById(R.id.tvAccountName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvAccountBalance = itemView.findViewById(R.id.tvAccountBalance);
        }
    }
}

