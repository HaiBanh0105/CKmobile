package com.example.officer.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.officer.Activity.customer_infor;
import com.example.officer.Activity.open_mortgage;
import com.example.officer.Activity.open_savings;
import com.example.officer.DepositDialogFragment;
import com.example.officer.R;
import com.example.officer.model.Customer;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        holder.imgDeposit.setOnClickListener(v -> {
            // 1. Tạo Dialog
            DepositDialogFragment dialog = DepositDialogFragment.newInstance(
                    customer.getCustomerId(),
                    customer.getName()
            );

            // 2. Gán Listener xử lý kết quả
            dialog.setListener(new DepositDialogFragment.OnDepositListener() {
                @Override
                public void onDepositConfirmed(String customerId, double amount, String content) {
                    // [QUAN TRỌNG] Code cộng tiền vào Firebase ở đây
                    performDepositTransaction(customerId, amount, content);
                }
            });

            // 3. Hiển thị Dialog
            // Lưu ý: context phải là AppCompatActivity để gọi getSupportFragmentManager()
            dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "DepositDialog");
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvCustomerId;
        ImageView imgAddMortgage, imgAddSaving, imgDeposit;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(com.example.officer.R.id.tvCustomerName);
            tvCustomerId = itemView.findViewById(R.id.tvCustomerId);
            imgAddMortgage = itemView.findViewById(R.id.imgAddMortgage);
            imgAddSaving = itemView.findViewById(R.id.imgAddSaving);
            imgDeposit = itemView.findViewById(R.id.imgDeposit);
        }
    }

    public void updateList(List<Customer> newList) {
        this.customers = newList;
        notifyDataSetChanged();
    }

    private void performDepositTransaction(String userId, double amount, String content) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        DocumentReference accountRef = document.getReference();

                        Double currentBalance = document.getDouble("balance");
                        if (currentBalance == null) currentBalance = 0.0;

                        double newBalance = currentBalance + amount;

                        accountRef.update("balance", newBalance)
                                .addOnSuccessListener(aVoid -> {
                                    // Ghi log hoặc hiển thị thông báo
                                    Log.d("Deposit", "Nạp tiền thành công: " + amount);
                                    Toast.makeText(context, "Đã nạp " + amount + " VNĐ", Toast.LENGTH_SHORT).show();

                                    // Optional: Ghi lịch sử giao dịch nếu có collection "Transactions"
                                    Map<String, Object> transaction = new HashMap<>();
                                    transaction.put("user_id", userId);
                                    transaction.put("amount", amount);
                                    transaction.put("type", "deposit");
                                    transaction.put("content", content);
                                    transaction.put("timestamp", FieldValue.serverTimestamp());

                                    db.collection("AccountTransactions").add(transaction);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Deposit", "Lỗi khi cập nhật số dư", e);
                                    Toast.makeText(context, "Lỗi khi nạp tiền", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(context, "Không tìm thấy tài khoản checking", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Deposit", "Lỗi khi truy vấn tài khoản", e);
                    Toast.makeText(context, "Lỗi kết nối dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }



}

