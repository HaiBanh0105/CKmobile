package com.example.banking.model;

public class Account {
    private String user_id;
    private String account_type;
    private String account_number;
    private String name;
    private Double balance;
    private String BankCode;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Account() {} // Firestore cần constructor rỗng


    public String getAccount_type() { return account_type; }
    public String getAccount_number() { return account_number; }
    public Double getBalance() { return balance; }

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