package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model class representing a one-time email verification record.
 */
public class EmailVerification {
    private int verificationId;
    private String email;
    private String code;
    private String purpose;
    private Timestamp expiresAt;
    private boolean isUsed;
    private Timestamp createdAt;

    public EmailVerification() {}

    public EmailVerification(int verificationId, String email, String code, String purpose, Timestamp expiresAt, boolean isUsed, Timestamp createdAt) {
        this.verificationId = verificationId;
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.isUsed = isUsed;
        this.createdAt = createdAt;
    }

    public int getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(int verificationId) {
        this.verificationId = verificationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
