package com.recruitment.model;

/**
 * Model representing a candidate's answer to a specific question during an assessment attempt.
 */
public class AssessmentAnswer {
    private int id;
    private int attemptId;
    private int questionId;
    private String selectedOption; // 'A', 'B', 'C', or 'D'
    private boolean correct;
    private int pointsAwarded;

    // Joined metadata for review
    private String questionText;
    private String correctOption;
    private String difficulty;

    public AssessmentAnswer() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAttemptId() { return attemptId; }
    public void setAttemptId(int attemptId) { this.attemptId = attemptId; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public int getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(int pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
