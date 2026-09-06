package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model representing an Online Technical Assessment.
 */
public class Assessment {
    private int assessmentId;
    private Integer jobId;
    private String title;
    private String description;
    private int passingScore; // percentage (e.g. 60%)
    private int timeLimitMinutes;
    private Timestamp createdAt;

    // Joined metadata
    private String jobTitle;
    private String companyName;
    private int totalQuestions;
    private int easyQuestionsCount;
    private int mediumQuestionsCount;
    private int hardQuestionsCount;

    public Assessment() {}

    public int getAssessmentId() { return assessmentId; }
    public void setAssessmentId(int assessmentId) { this.assessmentId = assessmentId; }

    public Integer getJobId() { return jobId; }
    public void setJobId(Integer jobId) { this.jobId = jobId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getEasyQuestionsCount() { return easyQuestionsCount; }
    public void setEasyQuestionsCount(int easyQuestionsCount) { this.easyQuestionsCount = easyQuestionsCount; }

    public int getMediumQuestionsCount() { return mediumQuestionsCount; }
    public void setMediumQuestionsCount(int mediumQuestionsCount) { this.mediumQuestionsCount = mediumQuestionsCount; }

    public int getHardQuestionsCount() { return hardQuestionsCount; }
    public void setHardQuestionsCount(int hardQuestionsCount) { this.hardQuestionsCount = hardQuestionsCount; }
}
