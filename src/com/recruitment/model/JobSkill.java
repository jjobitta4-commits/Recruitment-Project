package com.recruitment.model;

/**
 * Model class representing a skill requirement for a specific job posting.
 * Maps to the normalized 'job_skills' table.
 */
public class JobSkill {
    private int id;
    private int jobId;
    private int skillId;
    private String skillName;
    private String category;
    private boolean isMandatory;
    private int minYearsRequired;

    public JobSkill() {
        this.isMandatory = true;
        this.minYearsRequired = 1;
    }

    public JobSkill(int skillId, String skillName, String category, boolean isMandatory, int minYearsRequired) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.isMandatory = isMandatory;
        this.minYearsRequired = minYearsRequired;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
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

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public int getMinYearsRequired() {
        return minYearsRequired;
    }

    public void setMinYearsRequired(int minYearsRequired) {
        this.minYearsRequired = minYearsRequired;
    }
}
