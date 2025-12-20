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

import com.example.banking.model.Customer;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {
    private List<Customer> customers;
    private Context context;

    public CustomerAdapter(Context context, List<Customer> customers) {
        this.context = context;
        this.customers = customers;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_row, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.tvCustomerName.setText(customer.getName());
        holder.tvCustomerId.setText("ID: " + customer.getCustomerId());

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, customer_infor.class);
            intent.putExtra("customer_ID", customer.getCustomerId());
            context.startActivity(intent);
        });

        holder.imgAddMortgage.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, open_mortgage.class);
            intent.putExtra("customer_ID", customer.getCustomerId());
            intent.putExtra("email", customer.getEmail());
            context.startActivity(intent);
        });

        holder.imgAddSaving.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, open_savings.class);
            intent.putExtra("customer_ID", customer.getCustomerId());
            intent.putExtra("email", customer.getEmail());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvCustomerId;
        ImageView imgAddMortgage, imgAddSaving;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvCustomerId = itemView.findViewById(R.id.tvCustomerId);
            imgAddMortgage = itemView.findViewById(R.id.imgAddMortgage);
            imgAddSaving = itemView.findViewById(R.id.imgAddSaving);
        }
    }
}

