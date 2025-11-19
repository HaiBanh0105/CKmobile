package com.example.banking.modal;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String idCard;

    public Customer() {} // cần cho Firestore

    public Customer(String name, String phone, String email, String address, String idCard) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.idCard = idCard;
    }

    // Getter & Setter
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getIdCard() { return idCard; }
}
