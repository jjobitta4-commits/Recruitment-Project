package com.recruitment.model;

/**
 * Model representing a candidate's declared skill with proficiency level and years of experience.
 */
public class CandidateSkill {
    private int id;
    private int candidateId;
    private int skillId;
    private String skillName;
    private String category;
    private String proficiencyLevel; // 'Beginner', 'Intermediate', 'Advanced', 'Expert'
    private int yearsExperience;

    public CandidateSkill() {}

    public CandidateSkill(int id, int candidateId, int skillId, String skillName, String category, String proficiencyLevel, int yearsExperience) {
        this.id = id;
        this.candidateId = candidateId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.category = category;
        this.proficiencyLevel = proficiencyLevel;
        this.yearsExperience = yearsExperience;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
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

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public int getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(int yearsExperience) {
        this.yearsExperience = yearsExperience;
    }
}
