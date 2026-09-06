package com.recruitment.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Model class representing a Job vacancy in the Smart Recruitment System.
 * Fully aligned with normalized MySQL 'jobs', 'companies', 'recruiters', and 'job_skills' tables.
 */
public class Job {
    private int jobId;
    private int companyId;
    private String company; // Company name
    private int recruiterId;
    private String recruiterName;
    private String title;
    private String description;
    private String jobType; // Full Time, Part Time, Contract, Internship, Remote
    private String location;
    private String country;
    private String salaryRange;
    private int minExperienceYears;
    private String minEducation;
    private int vacancies;
    private Date deadline;
    private String approvalStatus; // pending, approved, rejected
    private String status; // Active, Closed
    private Timestamp postedDate;

    // Relational Job Skills
    private List<JobSkill> jobSkills = new ArrayList<>();
    private List<JobSkill> mandatorySkills = new ArrayList<>();
    private List<JobSkill> preferredSkills = new ArrayList<>();

    // Backward-compatibility & Presentation convenience
    private String skillsRequired;
    private String educationRequired;
    private String experienceRequired;
    private int applicationCount;

    // Innovation 1 & 2: Skill Matching Engine fields
    private int matchScore;
    private String matchLevel;
    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    public Job() {
        this.vacancies = 1;
        this.approvalStatus = "approved";
        this.status = "Active";
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public int getMinExperienceYears() {
        return minExperienceYears;
    }

    public void setMinExperienceYears(int minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
        if (this.experienceRequired == null || this.experienceRequired.isEmpty()) {
            this.experienceRequired = minExperienceYears > 0 ? (minExperienceYears + "+ Years") : "Freshers / Any";
        }
    }

    public String getMinEducation() {
        return minEducation;
    }

    public void setMinEducation(String minEducation) {
        this.minEducation = minEducation;
        if (this.educationRequired == null || this.educationRequired.isEmpty()) {
            this.educationRequired = minEducation;
        }
    }

    public int getVacancies() {
        return vacancies;
    }

    public void setVacancies(int vacancies) {
        this.vacancies = vacancies;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(Timestamp postedDate) {
        this.postedDate = postedDate;
    }

    public List<JobSkill> getJobSkills() {
        return jobSkills;
    }

    public void setJobSkills(List<JobSkill> jobSkills) {
        this.jobSkills = jobSkills != null ? jobSkills : new ArrayList<>();
        this.mandatorySkills = new ArrayList<>();
        this.preferredSkills = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (JobSkill js : this.jobSkills) {
            if (js.isMandatory()) {
                this.mandatorySkills.add(js);
            } else {
                this.preferredSkills.add(js);
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(js.getSkillName());
        }
        if (this.skillsRequired == null || this.skillsRequired.isEmpty()) {
            this.skillsRequired = sb.toString();
        }
    }

    public List<JobSkill> getMandatorySkills() {
        return mandatorySkills;
    }

    public void setMandatorySkills(List<JobSkill> mandatorySkills) {
        this.mandatorySkills = mandatorySkills != null ? mandatorySkills : new ArrayList<>();
    }

    public List<JobSkill> getPreferredSkills() {
        return preferredSkills;
    }

    public void setPreferredSkills(List<JobSkill> preferredSkills) {
        this.preferredSkills = preferredSkills != null ? preferredSkills : new ArrayList<>();
    }

    public String getSkillsRequired() {
        return skillsRequired;
    }

    public void setSkillsRequired(String skillsRequired) {
        this.skillsRequired = skillsRequired;
    }

    public String getEducationRequired() {
        return educationRequired != null ? educationRequired : minEducation;
    }

    public void setEducationRequired(String educationRequired) {
        this.educationRequired = educationRequired;
        if (this.minEducation == null || this.minEducation.isEmpty()) {
            this.minEducation = educationRequired;
        }
    }

    public String getExperienceRequired() {
        return experienceRequired != null ? experienceRequired : (minExperienceYears > 0 ? minExperienceYears + "+ Years" : "Freshers / Any");
    }

    public void setExperienceRequired(String experienceRequired) {
        this.experienceRequired = experienceRequired;
    }

    public int getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
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
}
