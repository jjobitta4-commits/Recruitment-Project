package com.recruitment.model;

/**
 * Model representing an Assessment Question with multi-tiered difficulty for adaptive testing.
 */
public class Question {
    private int questionId;
    private int assessmentId;
    private Integer skillId;
    private String skillName;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption; // 'A', 'B', 'C', or 'D' (masked in client-facing payloads)
    private String difficulty;    // 'Easy', 'Medium', or 'Hard'
    private int points;           // 10 for Easy, 15 for Medium, 20 for Hard

    public Question() {}

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getAssessmentId() { return assessmentId; }
    public void setAssessmentId(int assessmentId) { this.assessmentId = assessmentId; }

    public Integer getSkillId() { return skillId; }
    public void setSkillId(Integer skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    /**
     * Creates a client-safe clone with the correct answer stripped to prevent client-side inspecting.
     */
    public Question toClientSafe() {
        Question safe = new Question();
        safe.setQuestionId(this.questionId);
        safe.setAssessmentId(this.assessmentId);
        safe.setSkillId(this.skillId);
        safe.setSkillName(this.skillName);
        safe.setQuestionText(this.questionText);
        safe.setOptionA(this.optionA);
        safe.setOptionB(this.optionB);
        safe.setOptionC(this.optionC);
        safe.setOptionD(this.optionD);
        safe.setDifficulty(this.difficulty);
        safe.setPoints(this.points);
        safe.setCorrectOption(null); // Stripped for security
        return safe;
    }
}
