package com.example.banking.model;

import com.google.firebase.Timestamp;
import java.util.Map;

public class ServiceBooking {

    private String bookingId;
    private String userId;
    private String serviceType; // e.g., "FLIGHT", "HOTEL", "MOVIE", "ECOMMERCE"
    private String status;
    private Double totalAmount;
    private String pnrCodeOrBookingRef;
    private Timestamp bookingTime;
    private String transactionId;
    private Map<String, Object> serviceDetails; // Dùng Map để lưu chi tiết linh hoạt

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public ServiceBooking() {}

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getPnrCodeOrBookingRef() { return pnrCodeOrBookingRef; }
    public void setPnrCodeOrBookingRef(String pnrCodeOrBookingRef) { this.pnrCodeOrBookingRef = pnrCodeOrBookingRef; }
    public Timestamp getBookingTime() { return bookingTime; }
    public void setBookingTime(Timestamp bookingTime) { this.bookingTime = bookingTime; }
    public Map<String, Object> getServiceDetails() { return serviceDetails; }
    public void setServiceDetails(Map<String, Object> serviceDetails) { this.serviceDetails = serviceDetails; }
}
