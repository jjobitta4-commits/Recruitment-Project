package com.recruitment.model;

/**
 * Model representing an education history record for a candidate.
 */
public class Education {
    private int educationId;
    private int candidateId;
    private String degree;
    private String institution;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
    private String gradeOrGpa;

    public Education() {}

    public Education(int educationId, int candidateId, String degree, String institution, String fieldOfStudy, Integer startYear, Integer endYear, String gradeOrGpa) {
        this.educationId = educationId;
        this.candidateId = candidateId;
        this.degree = degree;
        this.institution = institution;
        this.fieldOfStudy = fieldOfStudy;
        this.startYear = startYear;
        this.endYear = endYear;
        this.gradeOrGpa = gradeOrGpa;
    }

    public int getEducationId() {
        return educationId;
    }

    public void setEducationId(int educationId) {
        this.educationId = educationId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public String getGradeOrGpa() {
        return gradeOrGpa;
    }

    public void setGradeOrGpa(String gradeOrGpa) {
        this.gradeOrGpa = gradeOrGpa;
    }
}
