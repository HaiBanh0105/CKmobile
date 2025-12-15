package com.example.banking.model;

import com.google.firebase.Timestamp;

public class AccountTransaction {

    private String transactionId;
    private String userId;
    private String type; // e.g., "TRANSFER_IN", "TRANSFER_OUT", "TOP_UP"
    private Double amount;
    private Double balanceAfter;
    private String description;
    private Timestamp timestamp;

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
