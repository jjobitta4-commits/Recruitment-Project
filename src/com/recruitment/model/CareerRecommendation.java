package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a candidate-specific Career Path Recommendation.
 * Encapsulates multi-milestone career ladder evaluation, compatibility scoring,
 * next frontier skills, and stepping-stone job postings.
 */
public class CareerRecommendation {
    private CareerPath careerPath;
    private int candidateId;
    private int matchScore; // 0 to 100%
    private String readinessRating; // HIGH_AFFINITY, MODERATE_AFFINITY, EXPLORATORY
    private String readinessLabel;

    private int currentMilestoneLevel; // 1, 2, 3, or 4
    private CareerMilestone currentMilestone;
    private CareerMilestone nextMilestone;
    private List<CareerMilestone> completedMilestones = new ArrayList<>();

    private List<String> acquiredSkills = new ArrayList<>();
    private List<String> nextFrontierSkills = new ArrayList<>();
    private List<String> allMissingSkills = new ArrayList<>();

    // Multi-factor breakdown
    private int skillsScore;      // 40% weight
    private int experienceScore;  // 25% weight
    private int projectScore;     // 15% weight
    private int assessmentScore;  // 10% weight
    private int educationScore;   // 10% weight

    private List<Job> matchingJobs = new ArrayList<>();
    private String trajectorySummary;

    public CareerRecommendation() {}

    public CareerPath getCareerPath() {
        return careerPath;
    }

    public void setCareerPath(CareerPath careerPath) {
        this.careerPath = careerPath;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getReadinessRating() {
        return readinessRating;
    }

    public void setReadinessRating(String readinessRating) {
        this.readinessRating = readinessRating;
    }

    public String getReadinessLabel() {
        return readinessLabel;
    }

    public void setReadinessLabel(String readinessLabel) {
        this.readinessLabel = readinessLabel;
    }

    public int getCurrentMilestoneLevel() {
        return currentMilestoneLevel;
    }

    public void setCurrentMilestoneLevel(int currentMilestoneLevel) {
        this.currentMilestoneLevel = currentMilestoneLevel;
    }

    public CareerMilestone getCurrentMilestone() {
        return currentMilestone;
    }

    public void setCurrentMilestone(CareerMilestone currentMilestone) {
        this.currentMilestone = currentMilestone;
    }

    public CareerMilestone getNextMilestone() {
        return nextMilestone;
    }

    public void setNextMilestone(CareerMilestone nextMilestone) {
        this.nextMilestone = nextMilestone;
    }

    public List<CareerMilestone> getCompletedMilestones() {
        return completedMilestones;
    }

    public void setCompletedMilestones(List<CareerMilestone> completedMilestones) {
        this.completedMilestones = completedMilestones;
    }

    public List<String> getAcquiredSkills() {
        return acquiredSkills;
    }

    public void setAcquiredSkills(List<String> acquiredSkills) {
        this.acquiredSkills = acquiredSkills;
    }

    public List<String> getNextFrontierSkills() {
        return nextFrontierSkills;
    }

    public void setNextFrontierSkills(List<String> nextFrontierSkills) {
        this.nextFrontierSkills = nextFrontierSkills;
    }

    public List<String> getAllMissingSkills() {
        return allMissingSkills;
    }

    public void setAllMissingSkills(List<String> allMissingSkills) {
        this.allMissingSkills = allMissingSkills;
    }

    public int getSkillsScore() {
        return skillsScore;
    }

    public void setSkillsScore(int skillsScore) {
        this.skillsScore = skillsScore;
    }

    public int getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(int experienceScore) {
        this.experienceScore = experienceScore;
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

    public int getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(int educationScore) {
        this.educationScore = educationScore;
    }

    public List<Job> getMatchingJobs() {
        return matchingJobs;
    }

    public void setMatchingJobs(List<Job> matchingJobs) {
        this.matchingJobs = matchingJobs;
    }

    public String getTrajectorySummary() {
        return trajectorySummary;
    }

    public void setTrajectorySummary(String trajectorySummary) {
        this.trajectorySummary = trajectorySummary;
    }
}
