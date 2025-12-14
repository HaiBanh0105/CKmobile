package com.example.banking;

public class Transaction {
    private String name;
    private String date;
    private double amount;
    private String tpye;

    public Transaction() {} // Firestore cần constructor rỗng

    public Transaction(String name, String date, double amount, String tpye) {
        this.name = name;
        this.date = date;
        this.amount = amount;
        this.tpye = tpye;
    }

    public String getName() { return name; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }

    public String getType() { return tpye; }
}
