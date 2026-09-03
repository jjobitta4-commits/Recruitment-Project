package com.recruitment.server;

import com.recruitment.dao.*;
import com.recruitment.model.Applicant;
import com.recruitment.model.Application;
import com.recruitment.model.Job;
import com.recruitment.model.Recruiter;
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
 * HTTP Handler for Job Applications (Applying, Viewing, Status Updates).
 */
public class ApplicationHandler implements HttpHandler {

    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
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
                    handleGetApplications(exchange);
                    break;
                case "POST":
                    handleApplyForJob(exchange);
                    break;
                case "PUT":
                    if (path.endsWith("/status")) {
                        handleUpdateStatus(exchange);
                    } else {
                        ResponseHelper.sendError(exchange, 404, "Endpoint not found");
                    }
                    break;
                default:
                    ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ApplicationHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error processing application.");
        }
    }

    private void handleGetApplications(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required to view applications.");
            return;
        }

        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());

        if (session.isApplicant()) {
            int applicantId = getOrResolveApplicantId(session);
            if (applicantId <= 0) {
                ResponseHelper.sendError(exchange, 404, "Applicant profile not found.");
                return;
            }
            List<Application> apps = applicationDAO.getApplicationsByApplicant(applicantId);
            ResponseHelper.sendSuccess(exchange, "Applications retrieved", apps);

        } else if (session.isRecruiter()) {
            int recruiterId = getOrResolveRecruiterId(session);
            if (recruiterId <= 0) {
                ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
                return;
            }

            Integer jobId = null;
            if (query.containsKey("jobId")) {
                try { jobId = Integer.parseInt(query.get("jobId")); } catch (Exception ignored) {}
            }
            String status = query.get("status");

            List<Application> apps = applicationDAO.getApplicationsByRecruiter(recruiterId, jobId, status);
            ResponseHelper.sendSuccess(exchange, "Recruiter applications retrieved", apps);

        } else {
            ResponseHelper.sendError(exchange, 403, "Invalid user role.");
        }
    }

    private void handleApplyForJob(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isApplicant()) {
            ResponseHelper.sendError(exchange, 403, "Only registered applicants can apply for jobs. Please log in as an applicant.");
            return;
        }

        int applicantId = getOrResolveApplicantId(session);
        if (applicantId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Applicant profile not found.");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        Map<String, Object> fields = new HashMap<>();
        String resumeFileName = null;

        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            byte[] bodyBytes = ResponseHelper.readBodyBytes(exchange);
            MultipartParser.MultipartResult mpResult = MultipartParser.parse(contentType, bodyBytes);
            fields.putAll(mpResult.getFields());

            MultipartParser.FileItem file = mpResult.getFirstFile();
            if (file != null && file.getContent() != null && file.getContent().length > 0) {
                String originalName = file.getFileName();
                if (!originalName.toLowerCase().endsWith(".pdf")) {
                    ResponseHelper.sendError(exchange, 400, "Only PDF format (.pdf) is supported for resumes.");
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
                resumeFileName = safeName;
                // Also update applicant's default resume if requested
                applicantDAO.updateResumeFile(applicantId, safeName);
            }
        } else {
            String body = ResponseHelper.readBody(exchange);
            fields = JSONUtil.parseObject(body);
        }

        int jobId = JSONUtil.getInt(fields, "jobId", 0);
        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Job ID is required.");
            return;
        }

        Job job = jobDAO.getJobById(jobId);
        if (job == null) {
            ResponseHelper.sendError(exchange, 404, "Job opening not found.");
            return;
        }

        if (!"Active".equalsIgnoreCase(job.getStatus())) {
            ResponseHelper.sendError(exchange, 400, "This job posting is closed for applications.");
            return;
        }

        // Duplicate prevention check
        if (applicationDAO.hasApplied(applicantId, jobId)) {
            ResponseHelper.sendError(exchange, 409, "You have already submitted an application for this job opening.");
            return;
        }

        // Resolve resume
        Applicant applicant = applicantDAO.getApplicantById(applicantId);
        if (resumeFileName == null) {
            resumeFileName = JSONUtil.getString(fields, "resumePath", null);
            if (resumeFileName == null && applicant != null) {
                resumeFileName = applicant.getResumeFile();
            }
        }

        String coverLetter = JSONUtil.getString(fields, "coverLetter", "");

        Application app = new Application();
        app.setApplicantId(applicantId);
        app.setJobId(jobId);
        app.setResumePath(resumeFileName != null ? resumeFileName : "default_resume.pdf");
        app.setCoverLetter(coverLetter);
        app.setStatus("Applied");

        boolean success = applicationDAO.applyForJob(app);
        if (!success) {
            ResponseHelper.sendError(exchange, 500, "Failed to submit job application.");
            return;
        }

        // Create notification for applicant
        notificationDAO.createNotification(session.getUserId(), "Application Submitted",
                "You have successfully applied for the position: " + job.getTitle() + " at " + job.getCompany() + ".");

        // Create notification for recruiter
        Recruiter recruiter = recruiterDAO.getRecruiterById(job.getRecruiterId());
        if (recruiter != null) {
            String applicantName = applicant != null ? applicant.getFullName() : "A candidate";
            notificationDAO.createNotification(recruiter.getUserId(), "New Application Received",
                    applicantName + " has submitted an application for '" + job.getTitle() + "'.");
        }

        ResponseHelper.sendSuccess(exchange, "Application submitted successfully!", app);
    }

    private void handleUpdateStatus(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can update candidate application status.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int applicationId = JSONUtil.getInt(data, "applicationId", 0);
        String newStatus = JSONUtil.getString(data, "status", "").trim();

        if (applicationId <= 0 || newStatus.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "applicationId and new status are required.");
            return;
        }

        Application app = applicationDAO.getApplicationById(applicationId);
        if (app == null) {
            ResponseHelper.sendError(exchange, 404, "Application not found.");
            return;
        }

        boolean success = applicationDAO.updateApplicationStatus(applicationId, newStatus);
        if (!success) {
            ResponseHelper.sendError(exchange, 500, "Failed to update application status.");
            return;
        }

        // Notify applicant of status change
        Applicant applicant = applicantDAO.getApplicantById(app.getApplicantId());
        if (applicant != null) {
            String notificationMsg = String.format("Your application for '%s' at '%s' has been updated to: %s.",
                    app.getJobTitle(), app.getCompany(), newStatus);
            notificationDAO.createNotification(applicant.getUserId(), "Application Status Updated", notificationMsg);
        }

        ResponseHelper.sendSuccess(exchange, "Application status updated to: " + newStatus);
    }

    private int getOrResolveApplicantId(SessionManager.UserSession session) {
        if (session.getApplicantId() != null && session.getApplicantId() > 0) {
            return session.getApplicantId();
        }
        Applicant ap = applicantDAO.getApplicantByUserId(session.getUserId());
        if (ap != null) {
            session.setApplicantId(ap.getApplicantId());
            return ap.getApplicantId();
        }
        return -1;
    }

    private int getOrResolveRecruiterId(SessionManager.UserSession session) {
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
