package com.ledger.salesmanager.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String fullName;
    private String username;
    private String gmail;
    private String phone;
    private String passwordHash;
    private Role role;
    private String profilePicturePath;
    private boolean active = true;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public User() {}

    public User(String fullName, String username, String gmail, String phone, String passwordHash, Role role) {
        this.fullName = fullName;
        this.username = username;
        this.gmail = gmail;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getGmail() { return gmail; }
    public void setGmail(String gmail) { this.gmail = gmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String p) { this.profilePicturePath = p; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String initials() {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }
}
