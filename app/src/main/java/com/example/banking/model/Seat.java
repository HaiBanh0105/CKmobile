package com.example.banking.model;

public class Seat {
    public String id;        // A1, A2, B3...
    public boolean booked;   // đã đặt hay chưa
    public boolean selected; // đang chọn

    public Seat(String id, boolean booked) {
        this.id = id;
        this.booked = booked;
        this.selected = false;
    }
}

