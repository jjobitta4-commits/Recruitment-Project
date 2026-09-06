package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model class representing a registered system user (Candidate, Recruiter, or Admin).
 * Supports secure salted SHA-256 password hashing and role-based access control.
 */
public class User {
    private int userId;
    private String email;
    private String passwordHash;
    private String salt;
    private String role; // 'candidate', 'recruiter', 'admin'
    private String status; // 'active', 'disabled'
    private boolean isVerified;
    private Timestamp createdAt;
    private Timestamp lastLogin;

    public User() {}

    public User(int userId, String email, String passwordHash, String salt, String role, String status, boolean isVerified, Timestamp createdAt, Timestamp lastLogin) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.status = status;
        this.isVerified = isVerified;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Compatibility getters & setters
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }
}
