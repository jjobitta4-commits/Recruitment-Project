package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model class representing a candidate's Job Application.
 */
public class Application {
    private int applicationId;
    private int applicantId;
    private int jobId;
    private String resumePath;
    private String coverLetter;
    private String status; // Applied, Under Review, Shortlisted, Interview Scheduled, Selected, Rejected
    private Timestamp appliedDate;
    private Timestamp updatedDate;

    // Joined fields for display
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantSkills;
    private int applicantExperience;
    private String applicantEducation;
    private String jobTitle;
    private String company;
    private String jobLocation;
    private String jobType;

    public Application() {}

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(int applicantId) {
        this.applicantId = applicantId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Timestamp appliedDate) {
        this.appliedDate = appliedDate;
    }

    public Timestamp getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Timestamp updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getApplicantSkills() {
        return applicantSkills;
    }

    public void setApplicantSkills(String applicantSkills) {
        this.applicantSkills = applicantSkills;
    }

    public int getApplicantExperience() {
        return applicantExperience;
    }

    public void setApplicantExperience(int applicantExperience) {
        this.applicantExperience = applicantExperience;
    }

    public String getApplicantEducation() {
        return applicantEducation;
    }

    public void setApplicantEducation(String applicantEducation) {
        this.applicantEducation = applicantEducation;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
}
