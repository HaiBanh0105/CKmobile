package com.example.banking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_history, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvTransactionName.setText(transaction.getName());
        holder.tvTransactionDate.setText(transaction.getDate());
        holder.tvTransactionAmount.setText(String.format("%,.0f VND", transaction.getAmount()));

        if (transaction.getAmount() < 0) {
            holder.tvTransactionAmount.setText("-" + String.format("%,.0f VND", transaction.getAmount()));
            holder.tvTransactionAmount.setTextColor(Color.RED);
        } else {
            holder.tvTransactionAmount.setText("+" + String.format("%,.0f VND", transaction.getAmount()));
            holder.tvTransactionAmount.setTextColor(Color.GREEN);
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionName, tvTransactionDate, tvTransactionAmount;


        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionName = itemView.findViewById(R.id.tvTransactionName);
            tvTransactionDate = itemView.findViewById(R.id.tvTransactionDate);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);

        }
    }
}

