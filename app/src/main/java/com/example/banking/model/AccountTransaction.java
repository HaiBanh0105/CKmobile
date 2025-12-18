package com.example.banking.model;

import com.google.firebase.Timestamp;

public class AccountTransaction {

    private String transactionId;
    private String userId;
    private String type;
    private Double amount;
    private Double balanceAfter;
    private String description;
    private Timestamp timestamp;

    private String receiverName;      // Tên người nhận hoặc Tên đơn vị (Vd: Vietnam Airlines)
    private String receiverAccountNumber; // Số tài khoản người nhận (nếu có)
    private String receiverBankName;   // Tên ngân hàng nhận (nếu có)
    private String category;

    private String status;        // PENDING, SUCCESS, FAILED
    private Boolean biometricRequired;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getBiometricRequired() {
        return biometricRequired;
    }

    public void setBiometricRequired(Boolean biometricRequired) {
        this.biometricRequired = biometricRequired;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReceiverBankName() {
        return receiverBankName;
    }

    public void setReceiverBankName(String receiverBankName) {
        this.receiverBankName = receiverBankName;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public void setReceiverAccountNumber(String receiverAccountNumber) {
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public AccountTransaction() {}

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
