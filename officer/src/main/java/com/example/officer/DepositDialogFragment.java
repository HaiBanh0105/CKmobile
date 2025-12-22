package com.example.officer;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class DepositDialogFragment extends DialogFragment {

    // Interface để trả kết quả về Activity/Fragment cha
    public interface OnDepositListener {
        void onDepositConfirmed(String customerId, double amount, String content);
    }

    private OnDepositListener listener;
    private String customerId;
    private String customerName;

    // Các View
    private TextView tvName, tvId;
    private TextInputEditText edtAmount, edtNote;
    private MaterialButton btnCancel, btnConfirm;

    // Constructor rỗng bắt buộc
    public DepositDialogFragment() {}

    // Hàm tạo static để truyền dữ liệu vào dễ dàng
    public static DepositDialogFragment newInstance(String id, String name) {
        DepositDialogFragment fragment = new DepositDialogFragment();
        Bundle args = new Bundle();
        args.putString("CUSTOMER_ID", id);
        args.putString("CUSTOMER_NAME", name);
        fragment.setArguments(args);
        return fragment;
    }

    // Set listener từ bên ngoài
    public void setListener(OnDepositListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_dialog_staff_deposit, null);

        // Lấy dữ liệu truyền vào
        if (getArguments() != null) {
            customerId = getArguments().getString("CUSTOMER_ID");
            customerName = getArguments().getString("CUSTOMER_NAME");
        }

        // Ánh xạ View
        tvName = view.findViewById(R.id.tvCustomerName);
        tvId = view.findViewById(R.id.tvCustomerId);
        edtAmount = view.findViewById(R.id.edtAmount);
        edtNote = view.findViewById(R.id.edtNote);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnConfirm = view.findViewById(R.id.btnConfirmDeposit);

        // Hiển thị thông tin
        tvName.setText(customerName);
        tvId.setText("ID: " + customerId);

        // Xử lý nút Hủy
        btnCancel.setOnClickListener(v -> dismiss());

        // Xử lý nút Xác nhận
        btnConfirm.setOnClickListener(v -> handleConfirm());

        builder.setView(view);
        return builder.create();
    }

    private void handleConfirm() {
        String amountStr = edtAmount.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            edtAmount.setError("Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            edtAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            edtAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        // Nếu hợp lệ, gọi callback về Activity cha xử lý (Lưu Firebase, v.v.)
        if (listener != null) {
            listener.onDepositConfirmed(customerId, amount, note);
        }

        dismiss();
    }
}