package com.example.banking.model;

public class Seat {
    public String id;
    public boolean booked;
    public boolean selected;

    public Seat(String id, boolean booked) {
        this.id = id;
        this.booked = booked;
        this.selected = false;
    }

    public String getId() { return id; }
    public boolean isBooked() { return booked; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}


