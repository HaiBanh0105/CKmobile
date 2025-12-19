package com.example.banking.model;

public class Account {
    private String account_id;
    private String account_type;
    private String account_number;
    private Double balance;

    private String BankCode;

    public Account() {} // Firestore cần constructor rỗng

    public Account(String account_id, String account_type, String account_number, Double balance) {
        this.account_id = account_id;
        this.account_type = account_type;
        this.account_number = account_number;
        this.balance = balance;
    }

    public String getAccount_id() { return account_id; }
    public String getAccount_type() { return account_type; }
    public String getAccount_number() { return account_number; }
    public Double getBalance() { return balance; }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    public void setAccount_type(String account_type) {
        this.account_type = account_type;
    }

    public void setAccount_number(String account_number) {
        this.account_number = account_number;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getBankCode() {
        return BankCode;
    }

    public void setBankCode(String bankCode) {
        BankCode = bankCode;
    }
}