package com.example.banking.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.Map;

public class Flight implements Serializable {

    private String id;
    private String flightNumber;
    private String origin;
    private String destination;
    private Timestamp departureTime;
    private Timestamp arrivalTime;
    private String airline;
    private String logoUrl;

    // Tất cả hạng ghế
    private Map<String, Map<String, Integer>> seatClass;

    // Hạng ghế đang được chọn (Economy / Business ...)
    private String selectedSeatClassKey;

    public Flight() {}

    // ===== GET / SET =====

    public Map<String, Map<String, Integer>> getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(Map<String, Map<String, Integer>> seatClass) {
        this.seatClass = seatClass;
    }

    public String getSelectedSeatClassKey() {
        return selectedSeatClassKey;
    }

    public void setSelectedSeatClassKey(String selectedSeatClassKey) {
        this.selectedSeatClassKey = selectedSeatClassKey;
    }

    // 👉 Lấy thông tin hạng ghế đang chọn
    public Map<String, Integer> getSelectedSeatClass() {
        if (seatClass == null || selectedSeatClassKey == null) return null;
        return seatClass.get(selectedSeatClassKey);
    }

    // ===== Các field khác =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Timestamp getDepartureTime() { return departureTime; }
    public void setDepartureTime(Timestamp departureTime) { this.departureTime = departureTime; }

    public Timestamp getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(Timestamp arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
