package com.example.banking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.model.AccountTransaction;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter
        extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<AccountTransaction> transactionList;

    public TransactionAdapter(List<AccountTransaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_history, parent, false);

        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TransactionViewHolder holder, int position) {

        AccountTransaction transaction = transactionList.get(position);

        // Ngày giao dịch
        if (transaction.getTimestamp() != null) {
            String date = new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            ).format(transaction.getTimestamp().toDate());
            holder.tvTransactionDate.setText(date);
        } else {
            holder.tvTransactionDate.setText("Đang xử lý");
        }

        // Tên giao dịch -> description
        String description = transaction.getDescription();
        holder.tvTransactionName.setText(
                description != null && !description.isEmpty()
                        ? description
                        : "Giao dịch"
        );

        // Xác định tiền vào / ra
        boolean isIncome = isIncome(transaction.getType());
        double amount = transaction.getAmount() != null
                ? transaction.getAmount()
                : 0;

        String amountText =
                (isIncome ? "+" : "-")
                        + String.format("%,.0f VND", amount);

        holder.tvTransactionAmount.setText(amountText);
        holder.tvTransactionAmount.setTextColor(
                isIncome ? Color.parseColor("#2E7D32")
                        : Color.parseColor("#C62828")
        );
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

    // Quy ước loại giao dịch
    private boolean isIncome(String type) {
        if (type == null) return false;

        return type.equalsIgnoreCase("TRANSFER_IN");
    }
}
