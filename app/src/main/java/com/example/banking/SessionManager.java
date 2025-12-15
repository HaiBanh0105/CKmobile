package com.example.banking;

public class SessionManager {
    private static SessionManager instance;
    private String userId;

    private String name;

    private String pin;
    private String email;

    private String avatarUrl;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUserId(String id) { this.userId = id; }
    public String getUserId() { return userId; }

    public void setUserName(String name) { this.name = name; }
    public String getUserName() { return name; }

    public void setPinNumber(String pin) { this.pin = pin; }
    public String getPinNumber() { return pin; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAvatarUrl() { return avatarUrl; }
}