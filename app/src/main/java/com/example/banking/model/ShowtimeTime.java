package com.example.banking.model;

public class ShowtimeTime {
    private String time;
    private boolean isSelected = false;

    public ShowtimeTime(String time) {
        this.time = time;
    }

    public String getTime() { return time; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}