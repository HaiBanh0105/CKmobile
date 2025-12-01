package com.example.banking;

public class SessionManager {
    private static SessionManager instance;
    private String userId;

    private String email;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUserId(String id) { this.userId = id; }
    public String getUserId() { return userId; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }
}