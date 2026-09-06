package com.recruitment.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model class representing a scheduled interview.
 */
public class Interview {
    private int interviewId;
    private int applicationId;
    private int recruiterId;
    private int candidateId;
    private int applicantId;
    private int jobId;
    private Date interviewDate;
    private String interviewTime;
    private String interviewType; // Online, Offline
    private String meetingLink;
    private String status; // Scheduled, Completed, Cancelled
    private String notes;
    private String feedback;
    private int rating;
    private String evaluationFeedback;
    private int technicalScore;
    private int communicationScore;
    private int problemSolvingScore;
    private int overallScore;
    private Timestamp createdAt;

    // Joined fields for display
    private String applicantName;
    private String applicantEmail;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String company;

    public Interview() {}

    public int getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(int interviewId) {
        this.interviewId = interviewId;
    }

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

    public Date getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(Date interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getInterviewTime() {
        return interviewTime;
    }

    public void setInterviewTime(String interviewTime) {
        this.interviewTime = interviewTime;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getRecruiterId() { return recruiterId; }
    public void setRecruiterId(int recruiterId) { this.recruiterId = recruiterId; }

    public int getCandidateId() { return candidateId > 0 ? candidateId : applicantId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; this.applicantId = candidateId; }

    public String getFeedback() { return feedback != null ? feedback : (evaluationFeedback != null ? evaluationFeedback : notes); }
    public void setFeedback(String feedback) { this.feedback = feedback; this.notes = feedback; this.evaluationFeedback = feedback; }

    public String getEvaluationFeedback() { return evaluationFeedback != null ? evaluationFeedback : feedback; }
    public void setEvaluationFeedback(String evaluationFeedback) { this.evaluationFeedback = evaluationFeedback; this.feedback = evaluationFeedback; }

    public int getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(int technicalScore) { this.technicalScore = technicalScore; }

    public int getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(int communicationScore) { this.communicationScore = communicationScore; }

    public int getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(int problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getCandidateName() { return candidateName != null ? candidateName : applicantName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; this.applicantName = candidateName; }

    public String getCandidateEmail() { return candidateEmail != null ? candidateEmail : applicantEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; this.applicantEmail = candidateEmail; }
}
