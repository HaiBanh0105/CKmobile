package com.example.banking.Activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.banking.Adapter.TransactionAdapter;
import com.example.banking.R;
import com.example.banking.model.SessionManager;
import com.example.banking.databinding.ActivityTransactionHistoryBinding;
import com.example.banking.model.AccountTransaction;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistory extends AppCompatActivity {

    ActivityTransactionHistoryBinding binding;
    FirebaseFirestore db;

    List<AccountTransaction> transactionList;
    TransactionAdapter adapter;

    Calendar fromCalendar, toCalendar;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTransactionHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        currentUserId = SessionManager.getInstance().getUserId();
        initDateDefault();
        setupToolbar();
        setupRecyclerView();
        setupEvents();

        // 🔥 Load mặc định: giao dịch hôm nay
        loadTransactions();
    }

    // ================= INIT =================

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();

        adapter = new TransactionAdapter(transactionList, transaction -> {
            // Sau này:
            // Intent -> TransactionDetailActivity
            // hoặc BottomSheetDialogFragment
        });

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
    }

    private void setupEvents() {
        binding.fromDate.setOnClickListener(v -> showDatePicker(true));
        binding.toDate.setOnClickListener(v -> showDatePicker(false));

        binding.btnSearch.setOnClickListener(v -> loadTransactions());
    }

    // ================= DATE =================

    private void initDateDefault() {
        fromCalendar = Calendar.getInstance();
        toCalendar = Calendar.getInstance();

        // Bắt đầu ngày hôm nay
        fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
        fromCalendar.set(Calendar.MINUTE, 0);
        fromCalendar.set(Calendar.SECOND, 0);

        // Kết thúc ngày hôm nay
        toCalendar.set(Calendar.HOUR_OF_DAY, 23);
        toCalendar.set(Calendar.MINUTE, 59);
        toCalendar.set(Calendar.SECOND, 59);

        updateDateText();
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar c = isFromDate ? fromCalendar : toCalendar;

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    c.set(year, month, dayOfMonth);

                    if (isFromDate) {
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                    } else {
                        c.set(Calendar.HOUR_OF_DAY, 23);
                        c.set(Calendar.MINUTE, 59);
                        c.set(Calendar.SECOND, 59);
                    }

                    updateDateText();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvFromDate.setText(sdf.format(fromCalendar.getTime()));
        binding.tvToDate.setText(sdf.format(toCalendar.getTime()));
    }

    // ================= FIRESTORE =================
    private void loadTransactions() {
        if (currentUserId == null) return;

        showEmptyState(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        transactionList.clear();
        adapter.notifyDataSetChanged();

        Date fromDate = fromCalendar.getTime();
        Date toDate = toCalendar.getTime();

        Query query = db.collection("AccountTransactions")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("status", "SUCCESS")
                .whereGreaterThanOrEqualTo("timestamp", fromDate)
                .whereLessThanOrEqualTo("timestamp", toDate)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        int checkedId = binding.radioGroupFilter.getCheckedRadioButtonId();

        // Danh sách INCOME
        List<String> incomeTypes = new ArrayList<>();
        incomeTypes.add("TRANSFER_IN");
        incomeTypes.add("WITHDRAW_SAVINGS");
        incomeTypes.add("MORTGAGE_DISBURSE");

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {

                        AccountTransaction t = doc.toObject(AccountTransaction.class);
                        if (t == null) continue;

                        t.setTransactionId(doc.getId());

                        // 🔥 FILTER TẠI CLIENT
                        if (checkedId == R.id.rb_income) {
                            if (incomeTypes.contains(t.getType())) {
                                transactionList.add(t);
                            }
                        } else if (checkedId == R.id.rb_outcome) {
                            if (!incomeTypes.contains(t.getType())) {
                                transactionList.add(t);
                            }
                        } else {
                            // TẤT CẢ
                            transactionList.add(t);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                    showEmptyState(transactionList.isEmpty());
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showEmptyState(true);
                    Toast.makeText(this,
                            "Lỗi tải dữ liệu",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmptyState(boolean show) {
        if (show) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
        }
    }
}
