package com.example.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.banking.Adapter.CustomerAdapter;
import com.example.banking.model.Customer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class staff_home extends Fragment {
    private RecyclerView rvCustomers;
    private CustomerAdapter adapter;
    private List<Customer> customerList = new ArrayList<>();
    SearchView searchView;

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
        searchView = view.findViewById(R.id.searchView);


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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Khi người dùng nhấn Enter/Search
                filterCustomers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Khi người dùng gõ chữ
                filterCustomers(newText);
                return true;
            }
        });

    }

    private void loadCustomers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    customerList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Customer customer = new Customer(
                                doc.getString("user_id"),
                                doc.getString("name"),
                                doc.getString("email")
                        );
                        customerList.add(customer);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void filterCustomers(String text) {
        List<Customer> filteredList = new ArrayList<>();
        for (Customer c : customerList) {
            if (c.getName().toLowerCase().contains(text.toLowerCase()) ||
                    c.getEmail().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(c);
            }
        }
        adapter.updateList(filteredList); // viết hàm updateList trong adapter
    }


    @Override
    public void onResume() {
        super.onResume();
        loadCustomers();
    }
}