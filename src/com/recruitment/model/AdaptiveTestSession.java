package com.recruitment.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time state holder for an active Adaptive Test Session (Core Innovation 4).
 * Manages question sequence, difficulty elevation/lowering, and points telemetry.
 */
public class AdaptiveTestSession {
    private int attemptId;
    private int assessmentId;
    private int candidateId;
    private String assessmentTitle;
    private String currentDifficulty; // 'Easy', 'Medium', or 'Hard'
    private int currentQuestionNumber;
    private int totalQuestionsLimit = 5; // Standard adaptive session question length
    private Question currentQuestion;
    private int consecutiveCorrect;
    private int consecutiveIncorrect;
    private List<String> difficultyTrajectory = new ArrayList<>();
    private List<Integer> answeredQuestionIds = new ArrayList<>();
    private int pointsEarned;
    private int maxPossiblePoints;
    private int passingScore;
    private boolean completed;
    private boolean passed;
    private String feedbackMessage;

    public AdaptiveTestSession() {}

    public int getAttemptId() { return attemptId; }
    public void setAttemptId(int attemptId) { this.attemptId = attemptId; }

    public int getAssessmentId() { return assessmentId; }
    public void setAssessmentId(int assessmentId) { this.assessmentId = assessmentId; }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public String getAssessmentTitle() { return assessmentTitle; }
    public void setAssessmentTitle(String assessmentTitle) { this.assessmentTitle = assessmentTitle; }

    public String getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(String currentDifficulty) { this.currentDifficulty = currentDifficulty; }

    public int getCurrentQuestionNumber() { return currentQuestionNumber; }
    public void setCurrentQuestionNumber(int currentQuestionNumber) { this.currentQuestionNumber = currentQuestionNumber; }

    public int getTotalQuestionsLimit() { return totalQuestionsLimit; }
    public void setTotalQuestionsLimit(int totalQuestionsLimit) { this.totalQuestionsLimit = totalQuestionsLimit; }

    public Question getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(Question currentQuestion) { this.currentQuestion = currentQuestion; }

    public int getConsecutiveCorrect() { return consecutiveCorrect; }
    public void setConsecutiveCorrect(int consecutiveCorrect) { this.consecutiveCorrect = consecutiveCorrect; }

    public int getConsecutiveIncorrect() { return consecutiveIncorrect; }
    public void setConsecutiveIncorrect(int consecutiveIncorrect) { this.consecutiveIncorrect = consecutiveIncorrect; }

    public List<String> getDifficultyTrajectory() { return difficultyTrajectory; }
    public void setDifficultyTrajectory(List<String> difficultyTrajectory) { this.difficultyTrajectory = difficultyTrajectory; }

    public List<Integer> getAnsweredQuestionIds() { return answeredQuestionIds; }
    public void setAnsweredQuestionIds(List<Integer> answeredQuestionIds) { this.answeredQuestionIds = answeredQuestionIds; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public int getMaxPossiblePoints() { return maxPossiblePoints; }
    public void setMaxPossiblePoints(int maxPossiblePoints) { this.maxPossiblePoints = maxPossiblePoints; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getFeedbackMessage() { return feedbackMessage; }
    public void setFeedbackMessage(String feedbackMessage) { this.feedbackMessage = feedbackMessage; }

    public int getPercentageScore() {
        if (maxPossiblePoints > 0) {
            return (int) Math.round(((double) pointsEarned / maxPossiblePoints) * 100.0);
        }
        return 0;
    }
}
