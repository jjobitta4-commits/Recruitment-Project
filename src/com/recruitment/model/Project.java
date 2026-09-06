package com.recruitment.model;

/**
 * Model representing a candidate's portfolio project.
 */
public class Project {
    private int projectId;
    private int candidateId;
    private String title;
    private String technologiesUsed;
    private String projectUrl;
    private String description;

    public Project() {}

    public Project(int projectId, int candidateId, String title, String technologiesUsed, String projectUrl, String description) {
        this.projectId = projectId;
        this.candidateId = candidateId;
        this.title = title;
        this.technologiesUsed = technologiesUsed;
        this.projectUrl = projectUrl;
        this.description = description;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTechnologiesUsed() {
        return technologiesUsed;
    }

    public void setTechnologiesUsed(String technologiesUsed) {
        this.technologiesUsed = technologiesUsed;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
