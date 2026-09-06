package com.recruitment.model;

/**
 * Represents an identified proficiency or experience depth deficiency in a skill
 * that the candidate has declared, but which falls short of the job's requirements.
 */
public class ProficiencyGap {
    private String skillName;
    private String currentProficiency;
    private int currentYears;
    private int requiredYears;
    private String gapDescription;

    public ProficiencyGap() {}

    public ProficiencyGap(String skillName, String currentProficiency, int currentYears, int requiredYears, String gapDescription) {
        this.skillName = skillName;
        this.currentProficiency = currentProficiency;
        this.currentYears = currentYears;
        this.requiredYears = requiredYears;
        this.gapDescription = gapDescription;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getCurrentProficiency() {
        return currentProficiency;
    }

    public void setCurrentProficiency(String currentProficiency) {
        this.currentProficiency = currentProficiency;
    }

    public int getCurrentYears() {
        return currentYears;
    }

    public void setCurrentYears(int currentYears) {
        this.currentYears = currentYears;
    }

    public int getRequiredYears() {
        return requiredYears;
    }

    public void setRequiredYears(int requiredYears) {
        this.requiredYears = requiredYears;
    }

    public String getGapDescription() {
        return gapDescription;
    }

    public void setGapDescription(String gapDescription) {
        this.gapDescription = gapDescription;
    }
}
