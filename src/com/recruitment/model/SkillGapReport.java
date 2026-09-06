package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the complete Skill Gap Analysis and Personalized Learning Roadmap
 * generated for a candidate targeting a specific job opening.
 * Core Innovation 2: Skill Gap Analyzer & Learning Path Recommendation.
 */
public class SkillGapReport {
    private int jobId;
    private String jobTitle;
    private String companyName;
    private int candidateId;
    private String candidateName;

    // Matching metrics
    private int overallScore;
    private String matchLevel;
    private int skillScore;
    private int experienceScore;
    private int educationScore;
    private int projectScore;
    private int assessmentScore;

    // Gaps identified
    private List<JobSkill> missingMandatorySkills = new ArrayList<>();
    private List<JobSkill> missingPreferredSkills = new ArrayList<>();
    private List<ProficiencyGap> proficiencyGaps = new ArrayList<>();
    private List<JobSkill> acquiredSkills = new ArrayList<>();

    // Generated Roadmap
    private List<RoadmapStep> learningRoadmap = new ArrayList<>();
    private int estimatedWeeksTotal;
    private String executiveSummary;

    public SkillGapReport() {}

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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

    public List<JobSkill> getMissingMandatorySkills() {
        return missingMandatorySkills;
    }

    public void setMissingMandatorySkills(List<JobSkill> missingMandatorySkills) {
        this.missingMandatorySkills = missingMandatorySkills != null ? missingMandatorySkills : new ArrayList<>();
    }

    public List<JobSkill> getMissingPreferredSkills() {
        return missingPreferredSkills;
    }

    public void setMissingPreferredSkills(List<JobSkill> missingPreferredSkills) {
        this.missingPreferredSkills = missingPreferredSkills != null ? missingPreferredSkills : new ArrayList<>();
    }

    public List<ProficiencyGap> getProficiencyGaps() {
        return proficiencyGaps;
    }

    public void setProficiencyGaps(List<ProficiencyGap> proficiencyGaps) {
        this.proficiencyGaps = proficiencyGaps != null ? proficiencyGaps : new ArrayList<>();
    }

    public List<JobSkill> getAcquiredSkills() {
        return acquiredSkills;
    }

    public void setAcquiredSkills(List<JobSkill> acquiredSkills) {
        this.acquiredSkills = acquiredSkills != null ? acquiredSkills : new ArrayList<>();
    }

    public List<RoadmapStep> getLearningRoadmap() {
        return learningRoadmap;
    }

    public void setLearningRoadmap(List<RoadmapStep> learningRoadmap) {
        this.learningRoadmap = learningRoadmap != null ? learningRoadmap : new ArrayList<>();
    }

    public int getEstimatedWeeksTotal() {
        return estimatedWeeksTotal;
    }

    public void setEstimatedWeeksTotal(int estimatedWeeksTotal) {
        this.estimatedWeeksTotal = estimatedWeeksTotal;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }
}
