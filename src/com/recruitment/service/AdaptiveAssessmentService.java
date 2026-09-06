package com.recruitment.service;

import com.recruitment.dao.AssessmentDAO;
import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.NotificationDAO;
import com.recruitment.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Innovation 4: Adaptive Online Assessment Engine.
 * Implements real-time dynamic difficulty steering (Easy <-> Medium <-> Hard)
 * based on candidate answer correctness trajectories.
 */
public class AdaptiveAssessmentService {

    private final AssessmentDAO assessmentDAO = new AssessmentDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // Active in-memory test sessions: attemptId -> AdaptiveTestSession
    private static final Map<Integer, AdaptiveTestSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    /**
     * Initiates an adaptive assessment session:
     * 1. Creates attempt in MySQL
     * 2. Sets baseline difficulty at 'Medium'
     * 3. Selects the first adaptive question
     * 4. Returns client-safe session payload
     */
    public AdaptiveTestSession startAdaptiveSession(int candidateId, int assessmentId) {
        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate profile not found.");
        }

        Assessment assessment = assessmentDAO.getAssessmentById(assessmentId);
        if (assessment == null) {
            throw new IllegalArgumentException("Assessment not found.");
        }

        // Create attempt in database
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentId(assessmentId);
        attempt.setCandidateId(candidateId);
        attempt.setTotalScore(0);
        attempt.setMaxScore(100);
        attempt.setDifficultyReached("Medium");
        attempt.setPassed(false);

        int attemptId = assessmentDAO.createAttempt(attempt);
        if (attemptId <= 0) {
            throw new RuntimeException("Failed to initialize test attempt.");
        }

        // Initialize active session
        AdaptiveTestSession session = new AdaptiveTestSession();
        session.setAttemptId(attemptId);
        session.setAssessmentId(assessmentId);
        session.setCandidateId(candidateId);
        session.setAssessmentTitle(assessment.getTitle());
        session.setPassingScore(assessment.getPassingScore());
        session.setCurrentDifficulty("Medium"); // Baseline starting tier
        session.setCurrentQuestionNumber(1);
        session.setTotalQuestionsLimit(5); // 5 adaptive questions per session

        // Pick initial question from Medium tier
        List<Question> mediumPool = assessmentDAO.getQuestionsByDifficulty(assessmentId, "Medium");
        Question initialQ;
        if (!mediumPool.isEmpty()) {
            initialQ = mediumPool.get(new Random().nextInt(mediumPool.size()));
        } else {
            // Fallback to Easy if no Medium questions
            List<Question> easyPool = assessmentDAO.getQuestionsByDifficulty(assessmentId, "Easy");
            if (easyPool.isEmpty()) {
                throw new IllegalStateException("No questions available for this assessment.");
            }
            initialQ = easyPool.get(0);
            session.setCurrentDifficulty("Easy");
        }

        session.setCurrentQuestion(initialQ.toClientSafe());
        session.setMaxPossiblePoints(initialQ.getPoints());
        session.setFeedbackMessage("Test initialized. Starting at " + session.getCurrentDifficulty() + " tier.");

        ACTIVE_SESSIONS.put(attemptId, session);
        return session;
    }

    /**
     * Submits candidate's answer to the current question:
     * 1. Evaluates correctness and awards points
     * 2. Persists answer into database
     * 3. Dynamically steers difficulty UP on correct, DOWN on incorrect
     * 4. Retrieves next unattempted question at the target tier
     * 5. Auto-finalizes if test length limit is reached
     */
    public AdaptiveTestSession submitAnswerAndSteer(int attemptId, int candidateId, int questionId, String selectedOption) {
        AdaptiveTestSession session = ACTIVE_SESSIONS.get(attemptId);
        if (session == null) {
            // Try rebuilding from database if server restarted
            session = rebuildSessionFromDb(attemptId, candidateId);
            if (session == null) {
                throw new IllegalArgumentException("Active test session not found or already completed.");
            }
            ACTIVE_SESSIONS.put(attemptId, session);
        }

        if (session.getCandidateId() != candidateId) {
            throw new SecurityException("Access denied. You do not own this test session.");
        }

        if (session.isCompleted()) {
            return session;
        }

        Question q = assessmentDAO.getQuestionById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("Question not found.");
        }

        // 1. Evaluate correctness
        boolean isCorrect = selectedOption != null &&
                q.getCorrectOption() != null &&
                selectedOption.trim().equalsIgnoreCase(q.getCorrectOption().trim());

        int points = isCorrect ? q.getPoints() : 0;

        // 2. Persist answer record
        AssessmentAnswer answer = new AssessmentAnswer();
        answer.setAttemptId(attemptId);
        answer.setQuestionId(questionId);
        answer.setSelectedOption(selectedOption != null ? selectedOption.trim().toUpperCase() : "");
        answer.setCorrect(isCorrect);
        answer.setPointsAwarded(points);
        assessmentDAO.recordAnswer(answer);

        // 3. Update session aggregates
        session.setPointsEarned(session.getPointsEarned() + points);
        session.getAnsweredQuestionIds().add(questionId);
        session.getDifficultyTrajectory().add(q.getDifficulty());

        // 4. Real-time Dynamic Difficulty Steering
        String currentTier = session.getCurrentDifficulty();
        String nextTier = currentTier;
        String feedback;

        if (isCorrect) {
            session.setConsecutiveCorrect(session.getConsecutiveCorrect() + 1);
            session.setConsecutiveIncorrect(0);

            if ("Easy".equalsIgnoreCase(currentTier)) {
                nextTier = "Medium";
                feedback = "✓ Correct! Difficulty elevated to Medium (+15 pts).";
            } else if ("Medium".equalsIgnoreCase(currentTier)) {
                nextTier = "Hard";
                feedback = "✓ Correct! Excellent performance — difficulty elevated to Hard (+20 pts).";
            } else {
                nextTier = "Hard";
                feedback = "✓ Correct! Outstanding mastery of the Hard tier (+20 pts).";
            }
        } else {
            session.setConsecutiveIncorrect(session.getConsecutiveIncorrect() + 1);
            session.setConsecutiveCorrect(0);

            if ("Hard".equalsIgnoreCase(currentTier)) {
                nextTier = "Medium";
                feedback = "✕ Incorrect. Difficulty adjusted to Medium.";
            } else if ("Medium".equalsIgnoreCase(currentTier)) {
                nextTier = "Easy";
                feedback = "✕ Incorrect. Difficulty adjusted to Easy.";
            } else {
                nextTier = "Easy";
                feedback = "✕ Incorrect. Continuing at Easy tier.";
            }
        }

        session.setCurrentDifficulty(nextTier);
        session.setFeedbackMessage(feedback);

        // 5. Check if test session complete
        int answeredCount = session.getAnsweredQuestionIds().size();
        if (answeredCount >= session.getTotalQuestionsLimit()) {
            return finalizeAssessmentSession(session);
        }

        // 6. Select next adaptive question matching nextTier
        Question nextQ = selectNextAdaptiveQuestion(session.getAssessmentId(), nextTier, session.getAnsweredQuestionIds());
        if (nextQ == null) {
            // If target tier exhausted, search adjacent tiers
            nextQ = selectFallbackQuestion(session.getAssessmentId(), session.getAnsweredQuestionIds());
        }

        if (nextQ == null) {
            // No more questions available in test pool -> finalize early
            return finalizeAssessmentSession(session);
        }

        session.setCurrentQuestionNumber(answeredCount + 1);
        session.setCurrentQuestion(nextQ.toClientSafe());
        session.setMaxPossiblePoints(session.getMaxPossiblePoints() + nextQ.getPoints());

        return session;
    }

    /**
     * Finalizes test attempt, computes percentage, updates MySQL database, and notifies candidate.
     */
    public AdaptiveTestSession finishAssessment(int attemptId, int candidateId) {
        AdaptiveTestSession session = ACTIVE_SESSIONS.get(attemptId);
        if (session == null) {
            session = rebuildSessionFromDb(attemptId, candidateId);
            if (session == null) {
                throw new IllegalArgumentException("Attempt not found.");
            }
        }
        return finalizeAssessmentSession(session);
    }

    private AdaptiveTestSession finalizeAssessmentSession(AdaptiveTestSession session) {
        session.setCompleted(true);
        session.setCurrentQuestion(null);

        int pointsEarned = session.getPointsEarned();
        int maxPoints = session.getMaxPossiblePoints();
        if (maxPoints <= 0) maxPoints = 50;

        int percentage = (int) Math.round(((double) pointsEarned / maxPoints) * 100.0);
        percentage = Math.min(100, Math.max(0, percentage));

        boolean isPassed = percentage >= session.getPassingScore();
        session.setPassed(isPassed);

        // Resolve highest difficulty reached
        String highestTier = "Easy";
        if (session.getDifficultyTrajectory().contains("Hard")) {
            highestTier = "Hard";
        } else if (session.getDifficultyTrajectory().contains("Medium")) {
            highestTier = "Medium";
        }
        session.setCurrentDifficulty(highestTier);

        // Update database attempt record
        assessmentDAO.finalizeAttempt(session.getAttemptId(), percentage, 100, highestTier, isPassed);

        // Send notification
        try {
            Candidate c = candidateDAO.getCandidateById(session.getCandidateId());
            if (c != null) {
                String outcome = isPassed ? "PASSED" : "NEEDS IMPROVEMENT";
                notificationDAO.createNotification(
                        c.getUserId(),
                        "Assessment Completed",
                        String.format("You scored %d%% on '%s' (%s). Peak difficulty reached: %s tier.",
                                percentage, session.getAssessmentTitle(), outcome, highestTier)
                );
            }
        } catch (Exception notifErr) {
            System.err.println("[AdaptiveAssessmentService] Notification notice: " + notifErr.getMessage());
        }

        session.setFeedbackMessage(String.format("Assessment completed! Final score: %d%% (%s). Highest tier reached: %s.",
                percentage, (isPassed ? "PASSED" : "NOT PASSED"), highestTier));

        ACTIVE_SESSIONS.remove(session.getAttemptId());
        return session;
    }

    private Question selectNextAdaptiveQuestion(int assessmentId, String difficulty, List<Integer> answeredIds) {
        List<Question> pool = assessmentDAO.getQuestionsByDifficulty(assessmentId, difficulty);
        for (Question q : pool) {
            if (!answeredIds.contains(q.getQuestionId())) {
                return q;
            }
        }
        return null;
    }

    private Question selectFallbackQuestion(int assessmentId, List<Integer> answeredIds) {
        List<Question> all = assessmentDAO.getQuestionsByAssessment(assessmentId);
        for (Question q : all) {
            if (!answeredIds.contains(q.getQuestionId())) {
                return q;
            }
        }
        return null;
    }

    private AdaptiveTestSession rebuildSessionFromDb(int attemptId, int candidateId) {
        AssessmentAttempt attempt = assessmentDAO.getAttemptById(attemptId);
        if (attempt == null || attempt.getCandidateId() != candidateId) {
            return null;
        }

        Assessment assessment = assessmentDAO.getAssessmentById(attempt.getAssessmentId());
        if (assessment == null) return null;

        AdaptiveTestSession session = new AdaptiveTestSession();
        session.setAttemptId(attemptId);
        session.setAssessmentId(attempt.getAssessmentId());
        session.setCandidateId(candidateId);
        session.setAssessmentTitle(assessment.getTitle());
        session.setPassingScore(assessment.getPassingScore());
        session.setCompleted(attempt.getCompletedAt() != null);
        session.setPassed(attempt.isPassed());

        List<AssessmentAnswer> answers = assessmentDAO.getAnswersByAttempt(attemptId);
        int earned = 0;
        int maxPts = 0;
        for (AssessmentAnswer a : answers) {
            earned += a.getPointsAwarded();
            session.getAnsweredQuestionIds().add(a.getQuestionId());
            session.getDifficultyTrajectory().add(a.getDifficulty());
            maxPts += (a.getPointsAwarded() > 0 ? a.getPointsAwarded() : 15);
        }
        session.setPointsEarned(earned);
        session.setMaxPossiblePoints(maxPts > 0 ? maxPts : 50);

        return session;
    }

    public List<Assessment> getAllAssessments() {
        return assessmentDAO.getAllAssessments();
    }

    public Assessment getAssessmentDetails(int assessmentId) {
        return assessmentDAO.getAssessmentById(assessmentId);
    }

    public List<AssessmentAttempt> getCandidateAttempts(int candidateId) {
        return assessmentDAO.getAttemptsByCandidate(candidateId);
    }

    public List<AssessmentAnswer> getAttemptAnswers(int attemptId) {
        return assessmentDAO.getAnswersByAttempt(attemptId);
    }
}
