package com.example.banking.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.banking.SessionManager;
import com.example.banking.databinding.TransferBinding;
import com.example.banking.model.AccountTransaction;
import com.example.banking.model.Bank;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class transfer extends BaseSecureActivity {
    final private Bank myBank = new Bank("WEST", "Westbank");
    private TransferBinding binding;
    private FirebaseFirestore db;

    private ListenerRegistration accountListener;
    private String receiverId;
    private String receiverName;
    private final List<Bank> bankList = new ArrayList<>();

    private Bank selectedBank = myBank;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = TransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initLoading(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.transfer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        loadSourceBalance();
        setupToolbar();
        setupTransferType();
        loadBanks();
        setupAccountWatcher();
        setupContinue();
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        binding.edtContent.setText(
                SessionManager.getInstance().getUserName() + " chuyển tiền"
        );
        binding.spinnerBanks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBank = bankList.get(position);
                lookupAccount(binding.edtAccountNumber.getText().toString().trim());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // ===== Loại chuyển khoản =====
    private void setupTransferType() {
        binding.chipGroupTransferType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(com.example.banking.R.id.chipExternal)) {
                binding.layoutBankSpinner.setVisibility(View.VISIBLE);
            } else {
                binding.layoutBankSpinner.setVisibility(View.GONE);
                selectedBank = myBank;
            }
        });
    }

    // ===== Load ngân hàng =====
    private void loadBanks() {
        db.collection("Banks")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    bankList.clear();
                    List<String> names = new ArrayList<>();

                    for (DocumentSnapshot doc : snap) {
                        Bank bank = doc.toObject(Bank.class);
                        if (bank != null) {
                            bankList.add(bank);
                            names.add(bank.getName());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            names
                    );
                    binding.spinnerBanks.setAdapter(adapter);
                });
    }

    // ===== Nhập STK =====
    private void setupAccountWatcher() {
        binding.edtAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                binding.cardAccountName.setVisibility(View.GONE);
                receiverId = null;

                if (s.length() == 12) {
                    lookupAccount(s.toString().trim());
                }
            }
        });
    }

    // ===== Tra cứu tài khoản =====
    private void lookupAccount(String accountNumber) {
        if (accountNumber.length() != 12) return;

        if (accountListener != null) accountListener.remove();

        Query query = db.collection("Accounts")
                .whereEqualTo("account_number", accountNumber);

        query = query.whereEqualTo("bankCode", selectedBank.getCode());

        accountListener = query.limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || snap.isEmpty()) {
                        showAccountNotFound();
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    receiverId = doc.getString("user_id");
                    receiverName = doc.getString("name");

                    binding.tvAccountNameResult.setText(receiverName);
                    binding.cardAccountName.setVisibility(View.VISIBLE);
                });
    }

    private void showAccountNotFound() {
        binding.tvAccountNameResult.setText("Không tìm thấy");
        binding.cardAccountName.setVisibility(View.VISIBLE);
    }

    // ===== Tạo transaction =====
    private void setupContinue() {
        binding.btnContinue.setOnClickListener(v -> {

            if (receiverId == null) {
                Toast.makeText(this, "Tài khoản không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (binding.edtAmount.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true);
            createTransaction();
        });
    }

    private void createTransaction() {
        String transactionId = UUID.randomUUID().toString();
        Double amount = Double.parseDouble(binding.edtAmount.getText().toString());

        AccountTransaction transaction = new AccountTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setUserId(SessionManager.getInstance().getUserId());
        transaction.setType("TRANSFER");
        transaction.setAmount(amount);
        transaction.setDescription(binding.edtContent.getText().toString());
        transaction.setTimestamp(Timestamp.now());

        transaction.setReceiverName(receiverName);
        transaction.setReceiverAccountNumber(binding.edtAccountNumber.getText().toString());
        transaction.setReceiverBankName(selectedBank.getName());

        transaction.setStatus("PENDING");
        transaction.setBiometricRequired(amount >= 5_000_000);

        db.collection("AccountTransactions")
                .document(transactionId)
                .set(transaction)
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, AccountTransactionActivity.class);
                    intent.putExtra("TRANSACTION_ID", transactionId);
                    startActivity(intent);
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không tạo được giao dịch", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                        }
                );
    }

    private void loadSourceBalance() {
        String userId = SessionManager.getInstance().getUserId();

        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking") // ví dụ tài khoản vãng lai
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        Double balance = snapshot.getDocuments().get(0).getDouble("balance");
                        binding.tvSourceBalance.setText("Số dư: " + formatCurrency(balance));
                    } else {
                        binding.tvSourceBalance.setText("Số dư: 0₫");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.tvSourceBalance.setText("Số dư: lỗi");
                });
    }

    private String formatCurrency(Double amount) {
        if (amount == null) amount = 0.0;
        return String.format("%,.0f₫", amount);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (accountListener != null) accountListener.remove();
    }
}
