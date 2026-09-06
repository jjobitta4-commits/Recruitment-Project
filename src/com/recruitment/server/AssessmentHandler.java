package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.model.*;
import com.recruitment.service.AdaptiveAssessmentService;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Enterprise HTTP Handler for Adaptive Assessments & Real-Time Test Steering.
 * Registered in MainServer at /api/assessments.
 */
public class AssessmentHandler implements HttpHandler {

    private final AdaptiveAssessmentService assessmentService = new AdaptiveAssessmentService();
    private final CandidateDAO candidateDAO = new CandidateDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET":
                    if (path.endsWith("/attempts") || path.contains("/attempts?")) {
                        handleGetAttempts(exchange);
                    } else if (path.matches(".*/api/assessments/attempts/\\d+")) {
                        handleGetAttemptReview(exchange, path);
                    } else if (path.matches(".*/api/assessments/\\d+")) {
                        handleGetAssessmentById(exchange, path);
                    } else {
                        handleGetAllAssessments(exchange);
                    }
                    break;

                case "POST":
                    if (path.endsWith("/start")) {
                        handleStartSession(exchange);
                    } else if (path.endsWith("/answer")) {
                        handleSubmitAnswer(exchange);
                    } else if (path.endsWith("/finish")) {
                        handleFinishSession(exchange);
                    } else if (path.endsWith("/questions")) {
                        handleAddQuestion(exchange);
                    } else {
                        handleCreateAssessment(exchange);
                    }
                    break;

                default:
                    ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            ResponseHelper.sendError(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            ResponseHelper.sendError(exchange, 403, e.getMessage());
        } catch (Exception e) {
            System.err.println("[AssessmentHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error in assessment engine: " + e.getMessage());
        }
    }

    private void handleGetAllAssessments(HttpExchange exchange) throws IOException {
        List<Assessment> list = assessmentService.getAllAssessments();
        ResponseHelper.sendSuccess(exchange, "Assessments retrieved successfully", list);
    }

    private void handleGetAssessmentById(HttpExchange exchange, String path) throws IOException {
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        Assessment a = assessmentService.getAssessmentDetails(id);
        if (a == null) {
            ResponseHelper.sendError(exchange, 404, "Assessment not found.");
            return;
        }
        ResponseHelper.sendSuccess(exchange, "Assessment details retrieved", a);
    }

    private void handleGetAttempts(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());

        if (session.isRecruiter() || session.isAdmin()) {
            if (query.containsKey("candidateId")) {
                try {
                    candidateId = Integer.parseInt(query.get("candidateId").trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Candidate not found.");
            return;
        }

        List<AssessmentAttempt> attempts = assessmentService.getCandidateAttempts(candidateId);
        ResponseHelper.sendSuccess(exchange, "Past assessment attempts retrieved", attempts);
    }

    private void handleGetAttemptReview(HttpExchange exchange, String path) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        int attemptId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        List<AssessmentAnswer> answers = assessmentService.getAttemptAnswers(attemptId);
        ResponseHelper.sendSuccess(exchange, "Attempt answers retrieved", answers);
    }

    /**
     * POST /api/assessments/start
     * Body: { "assessmentId": 1 }
     */
    private void handleStartSession(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isApplicant() && !session.isCandidate())) {
            ResponseHelper.sendError(exchange, 403, "Only candidates can take online assessments.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        if (candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Candidate profile not found.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int assessmentId = JSONUtil.getInt(data, "assessmentId", 0);

        if (assessmentId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid assessmentId is required.");
            return;
        }

        AdaptiveTestSession activeSession = assessmentService.startAdaptiveSession(candidateId, assessmentId);
        ResponseHelper.sendSuccess(exchange, "Adaptive assessment started!", activeSession);
    }

    /**
     * POST /api/assessments/answer
     * Body: { "attemptId": 1, "questionId": 3, "selectedOption": "B" }
     */
    private void handleSubmitAnswer(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isApplicant() && !session.isCandidate())) {
            ResponseHelper.sendError(exchange, 403, "Authentication required.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int attemptId = JSONUtil.getInt(data, "attemptId", 0);
        int questionId = JSONUtil.getInt(data, "questionId", 0);
        String selectedOption = JSONUtil.getString(data, "selectedOption", "").trim().toUpperCase();

        if (attemptId <= 0 || questionId <= 0 || selectedOption.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "attemptId, questionId, and selectedOption ('A', 'B', 'C', or 'D') are required.");
            return;
        }

        AdaptiveTestSession nextSession = assessmentService.submitAnswerAndSteer(attemptId, candidateId, questionId, selectedOption);
        ResponseHelper.sendSuccess(exchange, nextSession.getFeedbackMessage(), nextSession);
    }

    /**
     * POST /api/assessments/finish
     * Body: { "attemptId": 1 }
     */
    private void handleFinishSession(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int attemptId = JSONUtil.getInt(data, "attemptId", 0);

        if (attemptId <= 0) {
            ResponseHelper.sendError(exchange, 400, "attemptId is required.");
            return;
        }

        AdaptiveTestSession finalSession = assessmentService.finishAssessment(attemptId, candidateId);
        ResponseHelper.sendSuccess(exchange, finalSession.getFeedbackMessage(), finalSession);
    }

    /**
     * POST /api/assessments
     * Admin/Recruiter creates an assessment.
     */
    private void handleCreateAssessment(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isRecruiter() && !session.isAdmin())) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters and administrators can create assessments.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        String title = JSONUtil.getString(data, "title", "").trim();
        String description = JSONUtil.getString(data, "description", "").trim();
        int passingScore = JSONUtil.getInt(data, "passingScore", 60);
        int timeLimit = JSONUtil.getInt(data, "timeLimitMinutes", 20);
        int jobId = JSONUtil.getInt(data, "jobId", 0);

        if (title.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Assessment title is required.");
            return;
        }

        Assessment a = new Assessment();
        a.setTitle(title);
        a.setDescription(description);
        a.setPassingScore(passingScore);
        a.setTimeLimitMinutes(timeLimit);
        if (jobId > 0) a.setJobId(jobId);

        com.recruitment.dao.AssessmentDAO dao = new com.recruitment.dao.AssessmentDAO();
        boolean success = dao.createAssessment(a);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Assessment created successfully", a);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to create assessment.");
        }
    }

    /**
     * POST /api/assessments/questions
     * Admin/Recruiter adds a question with specified difficulty ('Easy', 'Medium', 'Hard').
     */
    private void handleAddQuestion(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isRecruiter() && !session.isAdmin())) {
            ResponseHelper.sendError(exchange, 403, "Access denied.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int assessmentId = JSONUtil.getInt(data, "assessmentId", 0);
        String questionText = JSONUtil.getString(data, "questionText", "").trim();
        String optionA = JSONUtil.getString(data, "optionA", "").trim();
        String optionB = JSONUtil.getString(data, "optionB", "").trim();
        String optionC = JSONUtil.getString(data, "optionC", "").trim();
        String optionD = JSONUtil.getString(data, "optionD", "").trim();
        String correctOption = JSONUtil.getString(data, "correctOption", "").trim().toUpperCase();
        String difficulty = JSONUtil.getString(data, "difficulty", "Medium").trim();
        int points = JSONUtil.getInt(data, "points", 15);

        if (assessmentId <= 0 || questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || correctOption.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Missing required question fields.");
            return;
        }

        Question q = new Question();
        q.setAssessmentId(assessmentId);
        q.setQuestionText(questionText);
        q.setOptionA(optionA);
        q.setOptionB(optionB);
        q.setOptionC(optionC);
        q.setOptionD(optionD);
        q.setCorrectOption(correctOption);
        q.setDifficulty(difficulty);
        q.setPoints(points);

        com.recruitment.dao.AssessmentDAO dao = new com.recruitment.dao.AssessmentDAO();
        boolean success = dao.createQuestion(q);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Question added successfully to " + difficulty + " tier", q);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to create question.");
        }
    }

    private int resolveCandidateId(SessionManager.UserSession session) {
        if (session.getCandidateId() != null && session.getCandidateId() > 0) {
            return session.getCandidateId();
        }
        Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
        if (c != null) {
            session.setCandidateId(c.getCandidateId());
            return c.getCandidateId();
        }
        return -1;
    }
}
