package com.example.banking;

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

}
