package com.example.banking.model;

public class Passenger {

    public String title;
    public String type; // ADULT | CHILD | INFANT

    public String fullName;
    public String idCard; // chỉ ADULT
    public String dob;    // CHILD + INFANT bắt buộc

    public Passenger() {}

    public Passenger(String title, String type) {
        this.title = title;
        this.type = type;
    }
}
