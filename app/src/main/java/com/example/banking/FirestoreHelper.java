package com.example.banking;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreHelper {
    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // Định nghĩa interface callback để trả dữ liệu về
    // Callback cho Customer
    public interface CustomerCallback {
        void onSuccess(String name, String phone, String email, String address, String id);
        void onFailure(String errorMessage);
    }

    // Callback cho Account
    public interface AccountCallback {
        void onSuccess(String number, Double balance);
        void onFailure(String errorMessage);
    }

    public void loadCustomerInfor(String userId, CustomerCallback callback) {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String email = documentSnapshot.getString("email");
                        String address = documentSnapshot.getString("address");
                        String id = documentSnapshot.getString("user_id");

                        callback.onSuccess(name, phone, email, address, id);
                    } else {
                        callback.onFailure("Không tìm thấy khách hàng!");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Lỗi tải dữ liệu: " + e.getMessage());
                });
    }

    public void loadCheckingInfor(String userId, AccountCallback callback) {
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Lấy document đầu tiên (giả sử mỗi user chỉ có 1 checking account)
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        String number = doc.getString("account_number");
                        Double balance = doc.getDouble("balance");

                        callback.onSuccess(number, balance);
                    } else {
                        callback.onFailure("Không tìm thấy tài khoản checking!");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Lỗi tải dữ liệu: " + e.getMessage());
                });
    }

    // Hàm thay đổi balance theo user_id
    public void changeCheckingBalanceByUserId(Context context, String userId, double amountChange) {
        db.collection("Accounts")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("account_type", "checking")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String docId = doc.getId();

                        Double currentBalance = doc.getDouble("balance");
                        if (currentBalance == null) currentBalance = 0.0;

                        double newBalance = currentBalance + amountChange;

                        // ✅ Kiểm tra số dư âm
                        if (newBalance < 0) {
                            Toast.makeText(
                                    context, // truyền context từ Activity/Fragment
                                    "Số dư không đủ để thực hiện giao dịch!",
                                    Toast.LENGTH_LONG
                            ).show();
                            return; // Dừng lại, không update Firestore
                        }

                        // ✅ Nếu hợp lệ thì update
                        db.collection("Accounts")
                                .document(docId)
                                .update("balance", newBalance)
//                                .addOnSuccessListener(aVoid -> {
//                                    Toast.makeText(
//                                            context,
//                                            "Cập nhật số dư thành công: " + newBalance,
//                                            Toast.LENGTH_SHORT
//                                    ).show();
//                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(
                                            context,
                                            "Lỗi khi cập nhật số dư!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                    } else {
                        Toast.makeText(
                                context,
                                "Không tìm thấy tài khoản checking cho user " + userId,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            context,
                            "Lỗi khi truy vấn Firestore!",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

}
