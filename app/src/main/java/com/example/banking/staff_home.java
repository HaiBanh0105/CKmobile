package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.modal.Customer;
import com.example.banking.viewHolder.CustomerViewHolder;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class staff_home extends Fragment {
    private FirebaseFirestore db;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.staff_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Lấy FloatingActionButton từ layout đã inflate
        FloatingActionButton fabAddCustomer = view.findViewById(R.id.fabAddCustomer);

        fabAddCustomer.setOnClickListener(v -> {
            // Mở EditCustomerActivity
            Intent intent = new Intent(requireContext(), customer_infor.class);
            startActivity(intent);
        });

        RecyclerView rvCustomers = view.findViewById(R.id.rvCustomers);
        rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));

        FirestoreRecyclerOptions<Customer> options =
                new FirestoreRecyclerOptions.Builder<Customer>()
                        .setQuery(db.collection("Customers"), Customer.class)
                        .build();

        FirestoreRecyclerAdapter<Customer, CustomerViewHolder> adapter =
                new FirestoreRecyclerAdapter<Customer, CustomerViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull CustomerViewHolder holder, int position, @NonNull Customer model) {
                        holder.tvName.setText(model.getName());
                        holder.tvId.setText("ID: " + model.getIdCard());
                    }

                    @NonNull
                    @Override
                    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_customer_row, parent, false);
                        return new CustomerViewHolder(view);
                    }
                };

        rvCustomers.setAdapter(adapter);
        adapter.startListening();

    }
}