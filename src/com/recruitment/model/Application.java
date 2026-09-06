package com.recruitment.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise model representing a Candidate's Job Application.
 * Aligns with the normalized `applications` MySQL schema, integrates with
 * the 5-factor Smart Job Matching Algorithm and Candidate Ranking System.
 */
public class Application {
    private int applicationId;
    private int jobId;
    private int candidateId;
    private Integer resumeId;
    private String coverLetter;
    private int matchScore; // 0 - 100 percentage calculated via 5-factor formula
    private String status;  // Applied, Under_Review, Shortlisted, Interview_Scheduled, Selected, Rejected
    private Timestamp appliedAt;
    private Timestamp updatedAt;

    // Joined Candidate Details
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateCity;
    private String candidateCountry;
    private String candidateEducation;
    private int candidateExperienceYears;
    private String candidateSkills;
    private int profileCompletion;

    // Joined Job & Company Details
    private String jobTitle;
    private String companyName;
    private String jobLocation;
    private String jobType;
    private String salaryRange;
    private String jobStatus;
    private int recruiterId;

    // Resume Details
    private String resumeFileName;
    private String resumePath;

    // Core Innovation 1 & 3: Match & Ranking Analytics
    private String matchLevel; // EXCELLENT, STRONG, MODERATE, LOW
    private int skillScore;
    private int experienceScore;
    private int educationScore;
    private int projectScore;
    private int assessmentScore;
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private List<String> missingMandatorySkills = new ArrayList<>();
    private List<String> missingPreferredSkills = new ArrayList<>();
    private boolean mandatoryPrerequisitesMet = true;

    // Candidate Ranking Fields
    private int rank; // 1-based ranking position for a job opening
    private String rankingInsight; // e.g. "🏆 Top Match (84%) • 5 Yrs Exp • All Prerequisites Met"

    public Application() {}

    // Primary Getters & Setters
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getJobId() { return jobId; }
    public void setJobId(int jobId) { this.jobId = jobId; }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public Integer getResumeId() { return resumeId; }
    public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Timestamp appliedAt) { this.appliedAt = appliedAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Joined Candidate Getters & Setters
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateCity() { return candidateCity; }
    public void setCandidateCity(String candidateCity) { this.candidateCity = candidateCity; }

    public String getCandidateCountry() { return candidateCountry; }
    public void setCandidateCountry(String candidateCountry) { this.candidateCountry = candidateCountry; }

    public String getCandidateEducation() { return candidateEducation; }
    public void setCandidateEducation(String candidateEducation) { this.candidateEducation = candidateEducation; }

    public int getCandidateExperienceYears() { return candidateExperienceYears; }
    public void setCandidateExperienceYears(int candidateExperienceYears) { this.candidateExperienceYears = candidateExperienceYears; }

    public String getCandidateSkills() { return candidateSkills; }
    public void setCandidateSkills(String candidateSkills) { this.candidateSkills = candidateSkills; }

    public int getProfileCompletion() { return profileCompletion; }
    public void setProfileCompletion(int profileCompletion) { this.profileCompletion = profileCompletion; }

    // Joined Job & Company Getters & Setters
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobLocation() { return jobLocation; }
    public void setJobLocation(String jobLocation) { this.jobLocation = jobLocation; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public String getJobStatus() { return jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }

    public int getRecruiterId() { return recruiterId; }
    public void setRecruiterId(int recruiterId) { this.recruiterId = recruiterId; }

    // Resume Getters & Setters
    public String getResumeFileName() { return resumeFileName; }
    public void setResumeFileName(String resumeFileName) { this.resumeFileName = resumeFileName; }

    public String getResumePath() { return resumePath != null ? resumePath : resumeFileName; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    // Innovation 1 & 3 Analytics Getters & Setters
    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public int getSkillScore() { return skillScore; }
    public void setSkillScore(int skillScore) { this.skillScore = skillScore; }

    public int getExperienceScore() { return experienceScore; }
    public void setExperienceScore(int experienceScore) { this.experienceScore = experienceScore; }

    public int getEducationScore() { return educationScore; }
    public void setEducationScore(int educationScore) { this.educationScore = educationScore; }

    public int getProjectScore() { return projectScore; }
    public void setProjectScore(int projectScore) { this.projectScore = projectScore; }

    public int getAssessmentScore() { return assessmentScore; }
    public void setAssessmentScore(int assessmentScore) { this.assessmentScore = assessmentScore; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getMissingMandatorySkills() { return missingMandatorySkills; }
    public void setMissingMandatorySkills(List<String> missingMandatorySkills) { this.missingMandatorySkills = missingMandatorySkills; }

    public List<String> getMissingPreferredSkills() { return missingPreferredSkills; }
    public void setMissingPreferredSkills(List<String> missingPreferredSkills) { this.missingPreferredSkills = missingPreferredSkills; }

    public boolean isMandatoryPrerequisitesMet() { return mandatoryPrerequisitesMet; }
    public void setMandatoryPrerequisitesMet(boolean mandatoryPrerequisitesMet) { this.mandatoryPrerequisitesMet = mandatoryPrerequisitesMet; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getRankingInsight() { return rankingInsight; }
    public void setRankingInsight(String rankingInsight) { this.rankingInsight = rankingInsight; }

    // ==========================================
    // Backward Compatibility Aliases for Legacy Code
    // ==========================================
    public int getApplicantId() { return candidateId; }
    public void setApplicantId(int applicantId) { this.candidateId = applicantId; }

    public String getApplicantName() { return candidateName; }
    public void setApplicantName(String applicantName) { this.candidateName = applicantName; }

    public String getApplicantEmail() { return candidateEmail; }
    public void setApplicantEmail(String applicantEmail) { this.candidateEmail = applicantEmail; }

    public String getApplicantPhone() { return candidatePhone; }
    public void setApplicantPhone(String applicantPhone) { this.candidatePhone = applicantPhone; }

    public String getApplicantSkills() { return candidateSkills; }
    public void setApplicantSkills(String applicantSkills) { this.candidateSkills = applicantSkills; }

    public int getApplicantExperience() { return candidateExperienceYears; }
    public void setApplicantExperience(int applicantExperience) { this.candidateExperienceYears = applicantExperience; }

    public String getApplicantEducation() { return candidateEducation; }
    public void setApplicantEducation(String applicantEducation) { this.candidateEducation = applicantEducation; }

    public String getCompany() { return companyName; }
    public void setCompany(String company) { this.companyName = company; }

    public Timestamp getAppliedDate() { return appliedAt; }
    public void setAppliedDate(Timestamp appliedDate) { this.appliedAt = appliedDate; }

    public Timestamp getUpdatedDate() { return updatedAt; }
    public void setUpdatedDate(Timestamp updatedDate) { this.updatedAt = updatedDate; }
}
