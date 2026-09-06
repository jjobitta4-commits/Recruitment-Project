package com.recruitment.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model representing a progressive Career Milestone / Level within a Career Path.
 * Aligned with 'career_milestones' table.
 */
public class CareerMilestone {
    private int milestoneId;
    private int pathId;
    private int levelOrder;
    private String levelName;
    private String experienceYearsRange;
    private String salaryRange;
    private String skillsRequired;
    private String milestoneDescription;
    private String recommendedAction;

    // Derived fields for candidate evaluation
    private boolean completed;
    private boolean isCurrent;
    private boolean isTarget;
    private List<String> skillsList = new ArrayList<>();
    private List<String> acquiredSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    public CareerMilestone() {}

    public CareerMilestone(int milestoneId, int pathId, int levelOrder, String levelName,
                           String experienceYearsRange, String salaryRange, String skillsRequired,
                           String milestoneDescription, String recommendedAction) {
        this.milestoneId = milestoneId;
        this.pathId = pathId;
        this.levelOrder = levelOrder;
        this.levelName = levelName;
        this.experienceYearsRange = experienceYearsRange;
        this.salaryRange = salaryRange;
        this.skillsRequired = skillsRequired;
        this.milestoneDescription = milestoneDescription;
        this.recommendedAction = recommendedAction;
        parseSkills();
    }

    public void parseSkills() {
        skillsList.clear();
        if (skillsRequired != null && !skillsRequired.trim().isEmpty()) {
            String[] parts = skillsRequired.split(",");
            for (String p : parts) {
                if (!p.trim().isEmpty()) {
                    skillsList.add(p.trim());
                }
            }
        }
    }

    public int getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(int milestoneId) {
        this.milestoneId = milestoneId;
    }

    public int getPathId() {
        return pathId;
    }

    public void setPathId(int pathId) {
        this.pathId = pathId;
    }

    public int getLevelOrder() {
        return levelOrder;
    }

    public void setLevelOrder(int levelOrder) {
        this.levelOrder = levelOrder;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public String getExperienceYearsRange() {
        return experienceYearsRange;
    }

    public void setExperienceYearsRange(String experienceYearsRange) {
        this.experienceYearsRange = experienceYearsRange;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getSkillsRequired() {
        return skillsRequired;
    }

    public void setSkillsRequired(String skillsRequired) {
        this.skillsRequired = skillsRequired;
        parseSkills();
    }

    public String getMilestoneDescription() {
        return milestoneDescription;
    }

    public void setMilestoneDescription(String milestoneDescription) {
        this.milestoneDescription = milestoneDescription;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setTarget(boolean target) {
        isTarget = target;
    }

    public List<String> getSkillsList() {
        if (skillsList.isEmpty() && skillsRequired != null) {
            parseSkills();
        }
        return skillsList;
    }

    public void setSkillsList(List<String> skillsList) {
        this.skillsList = skillsList;
    }

    public List<String> getAcquiredSkills() {
        return acquiredSkills;
    }

    public void setAcquiredSkills(List<String> acquiredSkills) {
        this.acquiredSkills = acquiredSkills;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }
}
