package com.example.banking.model;

public class Customer {
    private String customerId;
    private String name;

    public Customer() {} // cần constructor rỗng cho Firestore

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
}

