package com.recruitment.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model representing an Industry Career Path / Track in RecruitHub.
 * Aligned with 'career_paths' table.
 */
public class CareerPath {
    private int pathId;
    private String title;
    private String category;
    private String description;
    private String requiredCoreSkills;
    private int minStartingExperienceYears;
    private String averageMarketSalary;
    private Timestamp createdAt;

    private List<CareerMilestone> milestones = new ArrayList<>();
    private List<String> coreSkillsList = new ArrayList<>();

    public CareerPath() {}

    public CareerPath(int pathId, String title, String category, String description,
                      String requiredCoreSkills, int minStartingExperienceYears,
                      String averageMarketSalary, Timestamp createdAt) {
        this.pathId = pathId;
        this.title = title;
        this.category = category;
        this.description = description;
        this.requiredCoreSkills = requiredCoreSkills;
        this.minStartingExperienceYears = minStartingExperienceYears;
        this.averageMarketSalary = averageMarketSalary;
        this.createdAt = createdAt;
        parseCoreSkills();
    }

    public void parseCoreSkills() {
        coreSkillsList.clear();
        if (requiredCoreSkills != null && !requiredCoreSkills.trim().isEmpty()) {
            String[] parts = requiredCoreSkills.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    coreSkillsList.add(p.trim());
                }
            }
        }
    }

    public int getPathId() {
        return pathId;
    }

    public void setPathId(int pathId) {
        this.pathId = pathId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequiredCoreSkills() {
        return requiredCoreSkills;
    }

    public void setRequiredCoreSkills(String requiredCoreSkills) {
        this.requiredCoreSkills = requiredCoreSkills;
        parseCoreSkills();
    }

    public int getMinStartingExperienceYears() {
        return minStartingExperienceYears;
    }

    public void setMinStartingExperienceYears(int minStartingExperienceYears) {
        this.minStartingExperienceYears = minStartingExperienceYears;
    }

    public String getAverageMarketSalary() {
        return averageMarketSalary;
    }

    public void setAverageMarketSalary(String averageMarketSalary) {
        this.averageMarketSalary = averageMarketSalary;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<CareerMilestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<CareerMilestone> milestones) {
        this.milestones = milestones;
    }

    public List<String> getCoreSkillsList() {
        if (coreSkillsList.isEmpty() && requiredCoreSkills != null) {
            parseCoreSkills();
        }
        return coreSkillsList;
    }

    public void setCoreSkillsList(List<String> coreSkillsList) {
        this.coreSkillsList = coreSkillsList;
    }
}
