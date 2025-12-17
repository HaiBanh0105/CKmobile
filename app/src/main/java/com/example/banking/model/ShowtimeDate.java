package com.example.banking.model;

public class ShowtimeDate {
    private String displayDate;
    private String fullDate;
    private String day;
    private boolean isSelected;

    public ShowtimeDate(String displayDate, String fullDate, String day) {
        this.displayDate = displayDate;
        this.fullDate = fullDate;
        this.day = day;
        this.isSelected = false;
    }

    public String getDisplayDate() { return displayDate; }
    public String getFullDate() { return fullDate; }
    public String getDay() { return day; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}


