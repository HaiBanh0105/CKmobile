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

import com.example.banking.model.Customer;
import com.example.banking.viewHolder.CustomerViewHolder;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class staff_home extends Fragment {
    private RecyclerView rvCustomers;
    private CustomerAdapter adapter;
    private List<Customer> customerList = new ArrayList<>();
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
            intent.putExtra("role", "customer_register");
            startActivity(intent);
        });

        rvCustomers = view.findViewById(R.id.rvCustomers);
        rvCustomers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CustomerAdapter(getContext(), customerList);
        rvCustomers.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadCustomers();
    }

    private void loadCustomers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .whereEqualTo("role", "customer")   // lọc theo role = customer
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customerList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // ánh xạ sang model Customer
                        Customer customer = new Customer(
                                doc.getString("user_id"),   // hoặc doc.getId() nếu bạn dùng id document
                                doc.getString("name")
                        );
                        customerList.add(customer);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Lỗi tải dữ liệu: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

}