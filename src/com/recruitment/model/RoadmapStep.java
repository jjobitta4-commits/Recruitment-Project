package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a single actionable step in the generated personalized Learning Roadmap.
 */
public class RoadmapStep {
    private int stepNumber;
    private String skillName;
    private String category;
    private String urgency;             // CRITICAL, RECOMMENDED, OPTIONAL
    private String actionTitle;
    private String actionDescription;
    private String estimatedTime;       // e.g. "2 Weeks (15 hrs/wk)"
    private List<Map<String, String>> resources = new ArrayList<>(); // e.g. [{ title, url, type }]
    private String suggestedProject;

    public RoadmapStep() {}

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getActionTitle() {
        return actionTitle;
    }

    public void setActionTitle(String actionTitle) {
        this.actionTitle = actionTitle;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public List<Map<String, String>> getResources() {
        return resources;
    }

    public void setResources(List<Map<String, String>> resources) {
        this.resources = resources != null ? resources : new ArrayList<>();
    }

    public String getSuggestedProject() {
        return suggestedProject;
    }

    public void setSuggestedProject(String suggestedProject) {
        this.suggestedProject = suggestedProject;
    }
}
