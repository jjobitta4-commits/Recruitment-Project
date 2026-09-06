package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class encapsulating the multi-factor weighted match computation.
 * Core Innovation 1: Smart Job Matching Algorithm.
 * Formula: Score = (Skill * 0.45) + (Exp * 0.20) + (Edu * 0.10) + (Project * 0.10) + (Assessment * 0.15)
 */
public class MatchResult {
    private int jobId;
    private int candidateId;
    private int overallScore;           // 0 - 100
    private String matchLevel;          // EXCELLENT, STRONG, MODERATE, LOW

    // 5 Dimension Sub-Scores
    private int skillScore;             // Weight: 45%
    private int experienceScore;        // Weight: 20%
    private int educationScore;         // Weight: 10%
    private int projectScore;           // Weight: 10%
    private int assessmentScore;        // Weight: 15%

    // Skill breakdown details
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private List<String> missingMandatorySkills = new ArrayList<>();
    private List<String> missingPreferredSkills = new ArrayList<>();

    // Qualitative notes & explanations
    private String matchSummary;
    private boolean mandatoryPrerequisitesMet;

    public MatchResult() {}

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public int getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(int skillScore) {
        this.skillScore = skillScore;
    }

    public int getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(int experienceScore) {
        this.experienceScore = experienceScore;
    }

    public int getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(int educationScore) {
        this.educationScore = educationScore;
    }

    public int getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(int projectScore) {
        this.projectScore = projectScore;
    }

    public int getAssessmentScore() {
        return assessmentScore;
    }

    public void setAssessmentScore(int assessmentScore) {
        this.assessmentScore = assessmentScore;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills != null ? missingSkills : new ArrayList<>();
    }

    public List<String> getMissingMandatorySkills() {
        return missingMandatorySkills;
    }

    public void setMissingMandatorySkills(List<String> missingMandatorySkills) {
        this.missingMandatorySkills = missingMandatorySkills != null ? missingMandatorySkills : new ArrayList<>();
    }

    public List<String> getMissingPreferredSkills() {
        return missingPreferredSkills;
    }

    public void setMissingPreferredSkills(List<String> missingPreferredSkills) {
        this.missingPreferredSkills = missingPreferredSkills != null ? missingPreferredSkills : new ArrayList<>();
    }

    public String getMatchSummary() {
        return matchSummary;
    }

    public void setMatchSummary(String matchSummary) {
        this.matchSummary = matchSummary;
    }

    public boolean isMandatoryPrerequisitesMet() {
        return mandatoryPrerequisitesMet;
    }

    public void setMandatoryPrerequisitesMet(boolean mandatoryPrerequisitesMet) {
        this.mandatoryPrerequisitesMet = mandatoryPrerequisitesMet;
    }
}
