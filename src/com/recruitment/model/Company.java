package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model class representing an enterprise/organization hiring via RecruitHub.
 * Maps to the normalized 'companies' table.
 */
public class Company {
    private int companyId;
    private String name;
    private String description;
    private String industry;
    private String website;
    private String location;
    private String approvalStatus; // pending, approved, rejected
    private Timestamp createdAt;

    public Company() {
        this.approvalStatus = "approved";
    }

    public Company(int companyId, String name, String industry, String location) {
        this.companyId = companyId;
        this.name = name;
        this.industry = industry;
        this.location = location;
        this.approvalStatus = "approved";
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
