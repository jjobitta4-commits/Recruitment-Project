package com.recruitment.model;

import java.sql.Timestamp;

/**
 * Model representing a candidate's Assessment Attempt session.
 */
public class AssessmentAttempt {
    private int attemptId;
    private int assessmentId;
    private int candidateId;
    private int totalScore;        // Points accrued
    private int maxScore;          // Maximum possible points
    private String difficultyReached; // Easy, Medium, Hard
    private boolean passed;
    private Timestamp completedAt;

    // Joined metadata
    private String assessmentTitle;
    private String candidateName;
    private int passingScore;
    private int percentageScore;
    private int totalQuestionsAnswered;

    public AssessmentAttempt() {}

    public int getAttemptId() { return attemptId; }
    public void setAttemptId(int attemptId) { this.attemptId = attemptId; }

    public int getAssessmentId() { return assessmentId; }
    public void setAssessmentId(int assessmentId) { this.assessmentId = assessmentId; }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

    public String getDifficultyReached() { return difficultyReached; }
    public void setDifficultyReached(String difficultyReached) { this.difficultyReached = difficultyReached; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    public String getAssessmentTitle() { return assessmentTitle; }
    public void setAssessmentTitle(String assessmentTitle) { this.assessmentTitle = assessmentTitle; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public int getPercentageScore() {
        if (maxScore > 0) {
            return (int) Math.round(((double) totalScore / maxScore) * 100.0);
        }
        return percentageScore;
    }
    public void setPercentageScore(int percentageScore) { this.percentageScore = percentageScore; }

    public int getTotalQuestionsAnswered() { return totalQuestionsAnswered; }
    public void setTotalQuestionsAnswered(int totalQuestionsAnswered) { this.totalQuestionsAnswered = totalQuestionsAnswered; }
}
