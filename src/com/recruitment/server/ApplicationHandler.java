package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.model.Candidate;
import com.recruitment.model.Recruiter;
import com.recruitment.model.Application;
import com.recruitment.service.ApplicationService;
import com.recruitment.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enterprise HTTP Handler for Job Applications and the Candidate Ranking System.
 * Connects directly to ApplicationService and uses standard com.sun.net.httpserver.HttpServer.
 */
public class ApplicationHandler implements HttpHandler {

    private final ApplicationService applicationService = new ApplicationService();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final String uploadsDir;

    public ApplicationHandler(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

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
                    if (path.endsWith("/stats")) {
                        handleGetApplicationStats(exchange);
                    } else if (path.matches(".*/api/applications/\\d+")) {
                        handleGetApplicationById(exchange, path);
                    } else {
                        handleGetApplications(exchange);
                    }
                    break;

                case "POST":
                    handleApplyForJob(exchange);
                    break;

                case "PUT":
                    if (path.endsWith("/status") || path.endsWith("/api/applications")) {
                        handleUpdateStatus(exchange);
                    } else {
                        ResponseHelper.sendError(exchange, 404, "Endpoint not found");
                    }
                    break;

                case "DELETE":
                    handleWithdrawApplication(exchange);
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
            System.err.println("[ApplicationHandler] Server error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error processing application: " + e.getMessage());
        }
    }

    /**
     * GET /api/applications
     * - Candidate: Retrieves submitted applications list with 5-factor compatibility.
     * - Recruiter: Invokes the Candidate Ranking System with multi-criteria sorting & filtering.
     */
    private void handleGetApplications(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());

        if (session.isApplicant() || session.isCandidate()) {
            int candidateId = resolveCandidateId(session);
            if (candidateId <= 0) {
                ResponseHelper.sendError(exchange, 404, "Candidate profile not found.");
                return;
            }

            List<Application> apps = applicationService.getApplicationsForCandidate(candidateId);
            ResponseHelper.sendSuccess(exchange, "Applications retrieved successfully", apps);

        } else if (session.isRecruiter()) {
            int recruiterId = resolveRecruiterId(session);
            if (recruiterId <= 0) {
                ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
                return;
            }

            Integer jobId = null;
            if (query.containsKey("jobId") && !query.get("jobId").trim().isEmpty()) {
                try {
                    jobId = Integer.parseInt(query.get("jobId").trim());
                } catch (NumberFormatException ignored) {}
            }

            String statusFilter = query.get("status");
            String sortBy = query.get("sortBy");
            if (sortBy == null || sortBy.trim().isEmpty()) {
                sortBy = query.get("sort"); // alias
            }

            List<Application> rankedCandidates = applicationService.rankCandidatesForJob(recruiterId, jobId, statusFilter, sortBy);
            ResponseHelper.sendSuccess(exchange, "Ranked candidates retrieved successfully", rankedCandidates);

        } else {
            ResponseHelper.sendError(exchange, 403, "Forbidden: Invalid user role.");
        }
    }

    /**
     * GET /api/applications/{id}
     */
    private void handleGetApplicationById(HttpExchange exchange, String path) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        int appId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        com.recruitment.dao.ApplicationDAO dao = new com.recruitment.dao.ApplicationDAO();
        Application app = dao.getApplicationById(appId);

        if (app == null) {
            ResponseHelper.sendError(exchange, 404, "Application not found.");
            return;
        }

        // Check ownership
        if (session.isApplicant() || session.isCandidate()) {
            int candidateId = resolveCandidateId(session);
            if (app.getCandidateId() != candidateId) {
                ResponseHelper.sendError(exchange, 403, "Access denied to this application.");
                return;
            }
        } else if (session.isRecruiter()) {
            int recruiterId = resolveRecruiterId(session);
            if (app.getRecruiterId() != recruiterId) {
                ResponseHelper.sendError(exchange, 403, "Access denied: You do not own this job vacancy.");
                return;
            }
        }

        ResponseHelper.sendSuccess(exchange, "Application details retrieved", app);
    }

    /**
     * GET /api/applications/stats
     */
    private void handleGetApplicationStats(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        if (session.isRecruiter()) {
            int recruiterId = resolveRecruiterId(session);
            Map<String, Integer> stats = applicationService.getApplicationStatsForRecruiter(recruiterId);
            ResponseHelper.sendSuccess(exchange, "Recruiter application stats retrieved", stats);
        } else if (session.isApplicant() || session.isCandidate()) {
            int candidateId = resolveCandidateId(session);
            Map<String, Integer> stats = applicationService.getApplicationStatsForCandidate(candidateId);
            ResponseHelper.sendSuccess(exchange, "Candidate application stats retrieved", stats);
        } else {
            ResponseHelper.sendError(exchange, 403, "Access denied.");
        }
    }

    /**
     * POST /api/applications
     * Candidate submits application. Automatically calculates and stores 5-factor match score.
     */
    private void handleApplyForJob(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isApplicant() && !session.isCandidate())) {
            ResponseHelper.sendError(exchange, 403, "Only registered candidates can submit job applications.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        if (candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Candidate profile not found.");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        Map<String, Object> fields = new HashMap<>();
        String uploadedResumeFileName = null;

        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            byte[] bodyBytes = ResponseHelper.readBodyBytes(exchange);
            MultipartParser.MultipartResult mpResult = MultipartParser.parse(contentType, bodyBytes);
            fields.putAll(mpResult.getFields());

            MultipartParser.FileItem file = mpResult.getFirstFile();
            if (file != null && file.getContent() != null && file.getContent().length > 0) {
                String originalName = file.getFileName();
                if (!originalName.toLowerCase().endsWith(".pdf")) {
                    ResponseHelper.sendError(exchange, 400, "Only PDF resumes (.pdf) are supported.");
                    return;
                }

                File resumeFolder = new File(uploadsDir, "resumes");
                if (!resumeFolder.exists()) {
                    resumeFolder.mkdirs();
                }

                String safeName = "resume_" + UUID.randomUUID().toString().substring(0, 8) + "_" +
                        originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File dest = new File(resumeFolder, safeName);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(file.getContent());
                }
                uploadedResumeFileName = safeName;
            }
        } else {
            String body = ResponseHelper.readBody(exchange);
            fields = JSONUtil.parseObject(body);
        }

        int jobId = JSONUtil.getInt(fields, "jobId", 0);
        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid jobId is required.");
            return;
        }

        String coverLetter = JSONUtil.getString(fields, "coverLetter", "");
        Integer resumeId = null;
        if (fields.containsKey("resumeId")) {
            resumeId = JSONUtil.getInt(fields, "resumeId", 0);
            if (resumeId <= 0) resumeId = null;
        }

        Application createdApp = applicationService.applyForJob(candidateId, jobId, coverLetter, resumeId, uploadedResumeFileName);
        ResponseHelper.sendSuccess(exchange, "Application submitted successfully with Smart Match rating of " + createdApp.getMatchScore() + "%!", createdApp);
    }

    /**
     * PUT /api/applications/status
     * Recruiter updates application review status.
     */
    private void handleUpdateStatus(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can update candidate application statuses.");
            return;
        }

        int recruiterId = resolveRecruiterId(session);
        if (recruiterId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int applicationId = JSONUtil.getInt(data, "applicationId", 0);
        String newStatus = JSONUtil.getString(data, "status", "").trim();

        if (applicationId <= 0 || newStatus.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Both applicationId and new status are required.");
            return;
        }

        boolean success = applicationService.updateApplicationStatus(recruiterId, applicationId, newStatus);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Application status successfully updated to: " + newStatus);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to update application status.");
        }
    }

    /**
     * DELETE /api/applications
     * Candidate withdraws an early-stage application.
     */
    private void handleWithdrawApplication(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isApplicant() && !session.isCandidate())) {
            ResponseHelper.sendError(exchange, 403, "Only registered candidates can withdraw their applications.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        if (candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Candidate profile not found.");
            return;
        }

        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
        int applicationId = 0;
        if (query.containsKey("applicationId")) {
            try {
                applicationId = Integer.parseInt(query.get("applicationId").trim());
            } catch (NumberFormatException ignored) {}
        }

        if (applicationId <= 0) {
            String body = ResponseHelper.readBody(exchange);
            if (body != null && !body.trim().isEmpty()) {
                Map<String, Object> data = JSONUtil.parseObject(body);
                applicationId = JSONUtil.getInt(data, "applicationId", 0);
            }
        }

        if (applicationId <= 0) {
            ResponseHelper.sendError(exchange, 400, "applicationId is required to withdraw an application.");
            return;
        }

        boolean success = applicationService.withdrawApplication(candidateId, applicationId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Application successfully withdrawn.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to withdraw application.");
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

    private int resolveRecruiterId(SessionManager.UserSession session) {
        if (session.getRecruiterId() != null && session.getRecruiterId() > 0) {
            return session.getRecruiterId();
        }
        Recruiter r = recruiterDAO.getRecruiterByUserId(session.getUserId());
        if (r != null) {
            session.setRecruiterId(r.getRecruiterId());
            return r.getRecruiterId();
        }
        return -1;
    }
}
