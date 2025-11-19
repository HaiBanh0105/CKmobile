package com.example.banking.viewHolder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.R;

public class CustomerViewHolder extends RecyclerView.ViewHolder {
    public TextView tvName;
    public TextView tvId;
    public CustomerViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvCustomerName);
        tvId = itemView.findViewById(R.id.tvCustomerId);
    }
}
