package com.example.banking.model;

import com.google.firebase.Timestamp;

public class AccountTransaction {

    private String transactionId;
    private String userId;
    private String type;             // Vd: "TRANSFER_OUT", "TRANSFER_IN", "BILL", "SERVICE"
    private Double amount;
    private Double balanceBefore;

    public Double getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(Double balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    private Double balanceAfter;
    private String description;
    private Timestamp timestamp;
    private String category;
    private String status;           // PENDING, SUCCESS, FAILED
    private Boolean biometricRequired;

    // Thông tin người nhận/đơn vị (khi bạn chuyển tiền)
    private String receiverName;
    private String receiverAccountNumber;
    private String receiverBankName;

    // THÊM: Thông tin người gửi (khi bạn nhận tiền)
    private String senderName;
    private String senderAccountNumber;
    private String senderBankName;

    // Constructor mặc định (bắt buộc cho Firebase)
    public AccountTransaction() {}

    // Getters and Setters (đã được cập nhật đầy đủ)

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
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverAccountNumber() { return receiverAccountNumber; }
    public void setReceiverAccountNumber(String receiverAccountNumber) { this.receiverAccountNumber = receiverAccountNumber; }
    public String getReceiverBankName() { return receiverBankName; }
    public void setReceiverBankName(String receiverBankName) { this.receiverBankName = receiverBankName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getBiometricRequired() { return biometricRequired; }
    public void setBiometricRequired(Boolean biometricRequired) { this.biometricRequired = biometricRequired; }

    // Getters and Setters cho thông tin người gửi mới
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAccountNumber() {
        return senderAccountNumber;
    }

    public void setSenderAccountNumber(String senderAccountNumber) {
        this.senderAccountNumber = senderAccountNumber;
    }

    public String getSenderBankName() {
        return senderBankName;
    }

    public void setSenderBankName(String senderBankName) {
        this.senderBankName = senderBankName;
    }
}
