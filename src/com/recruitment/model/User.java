package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model class representing a registered system user (Applicant or Recruiter).
 */
public class User {
    private int userId;
    private String email;
    private String password;
    private String role; // "applicant" or "recruiter"
    private Timestamp createdAt;

    public User() {}

    public User(int userId, String email, String password, String role, Timestamp createdAt) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
