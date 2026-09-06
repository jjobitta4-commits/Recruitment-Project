package com.recruitment.server;

import com.recruitment.dao.*;
import com.recruitment.model.Applicant;
import com.recruitment.model.Application;
import com.recruitment.model.Candidate;
import com.recruitment.model.Interview;
import com.recruitment.model.Job;
import com.recruitment.model.Recruiter;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Interview scheduling and lifecycle management.
 */
public class InterviewHandler implements HttpHandler {

    private final InterviewDAO interviewDAO = new InterviewDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

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
                    handleGetInterviews(exchange);
                    break;
                case "POST":
                    handleScheduleInterview(exchange);
                    break;
                case "PUT":
                    if (path.endsWith("/status")) {
                        handleUpdateStatus(exchange);
                    } else if (path.endsWith("/evaluate")) {
                        handleEvaluateInterview(exchange);
                    } else {
                        ResponseHelper.sendError(exchange, 404, "Endpoint not found");
                    }
                    break;
                default:
                    ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (Exception e) {
            System.err.println("[InterviewHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error in interview operations.");
        }
    }

    private void handleGetInterviews(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        if (session.isApplicant()) {
            int applicantId = getOrResolveApplicantId(session);
            List<Interview> list = interviewDAO.getInterviewsByApplicant(applicantId);
            ResponseHelper.sendSuccess(exchange, "Applicant interviews retrieved", list);

        } else if (session.isRecruiter()) {
            int recruiterId = getOrResolveRecruiterId(session);
            List<Interview> list = interviewDAO.getInterviewsByRecruiter(recruiterId);
            ResponseHelper.sendSuccess(exchange, "Recruiter interviews retrieved", list);

        } else {
            ResponseHelper.sendError(exchange, 403, "Forbidden");
        }
    }

    private void handleScheduleInterview(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can schedule interviews.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int applicationId = JSONUtil.getInt(data, "applicationId", 0);
        String dateStr = JSONUtil.getString(data, "interviewDate", "");
        String timeStr = JSONUtil.getString(data, "interviewTime", "");
        String type = JSONUtil.getString(data, "interviewType", "Online");
        String meetingLink = JSONUtil.getString(data, "meetingLink", "");
        String notes = JSONUtil.getString(data, "notes", "");

        if (applicationId <= 0 || dateStr.isEmpty() || timeStr.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "applicationId, interviewDate, and interviewTime are required.");
            return;
        }

        Application app = applicationDAO.getApplicationById(applicationId);
        if (app == null) {
            ResponseHelper.sendError(exchange, 404, "Application not found.");
            return;
        }

        Date parsedDate = parseDateSafely(dateStr);
        if (parsedDate == null) {
            ResponseHelper.sendError(exchange, 400, "Invalid date format. Expected YYYY-MM-DD or DD-MM-YYYY.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        if (recruiterId <= 0) {
            recruiterId = app.getRecruiterId();
        }

        Interview iv = new Interview();
        iv.setApplicationId(applicationId);
        iv.setRecruiterId(recruiterId);
        iv.setCandidateId(app.getCandidateId());
        iv.setApplicantId(app.getCandidateId());
        iv.setJobId(app.getJobId());
        iv.setInterviewDate(parsedDate);
        iv.setInterviewTime(timeStr);
        iv.setInterviewType(type);
        iv.setMeetingLink(meetingLink);
        iv.setStatus("Scheduled");
        iv.setNotes(notes);
        iv.setFeedback(notes);

        boolean success = interviewDAO.scheduleInterview(iv);
        if (!success) {
            ResponseHelper.sendError(exchange, 500, "Failed to schedule interview.");
            return;
        }

        // Update application status to "Interview Scheduled"
        applicationDAO.updateApplicationStatus(applicationId, "Interview Scheduled");

        // Notify candidate/applicant
        Candidate cand = candidateDAO.getCandidateById(app.getCandidateId());
        int notifyUserId = (cand != null) ? cand.getUserId() : 0;
        if (notifyUserId <= 0) {
            Applicant applicant = applicantDAO.getApplicantById(app.getApplicantId());
            if (applicant != null) notifyUserId = applicant.getUserId();
        }

        if (notifyUserId > 0) {
            String companyName = app.getCompany() != null ? app.getCompany() : "Hiring Company";
            String msg = String.format("An %s interview has been scheduled for '%s' at '%s' on %s at %s. Link/Location: %s",
                    type, app.getJobTitle(), companyName, dateStr, timeStr,
                    (meetingLink.isEmpty() ? "See interview details in portal" : meetingLink));
            notificationDAO.createNotification(notifyUserId, "Interview Scheduled", msg);
        }

        ResponseHelper.sendSuccess(exchange, "Interview scheduled successfully!", iv);
    }

    private void handleUpdateStatus(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can update interviews.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int interviewId = JSONUtil.getInt(data, "interviewId", 0);
        String status = JSONUtil.getString(data, "status", "").trim();

        if (interviewId <= 0 || status.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "interviewId and status are required.");
            return;
        }

        boolean success = interviewDAO.updateInterviewStatus(interviewId, status);
        if (success) {
            Interview iv = interviewDAO.getInterviewById(interviewId);
            if (iv != null) {
                Applicant applicant = applicantDAO.getApplicantById(iv.getApplicantId());
                if (applicant != null) {
                    notificationDAO.createNotification(applicant.getUserId(), "Interview Status Update",
                            "Your scheduled interview for '" + iv.getJobTitle() + "' has been marked as: " + status);
                }
            }
            ResponseHelper.sendSuccess(exchange, "Interview status updated to: " + status);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to update interview status.");
        }
    }

    private void handleEvaluateInterview(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Only recruiters can submit interview evaluations.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int interviewId = JSONUtil.getInt(data, "interviewId", 0);
        int rating = JSONUtil.getInt(data, "rating", 5);
        String feedback = JSONUtil.getString(data, "feedback", "");
        String status = JSONUtil.getString(data, "status", "Completed");
        String applicationStatus = JSONUtil.getString(data, "applicationStatus", "");

        if (interviewId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Invalid interviewId.");
            return;
        }

        boolean ok = interviewDAO.saveEvaluation(interviewId, rating, feedback, status);
        if (ok) {
            Interview iv = interviewDAO.getInterviewById(interviewId);
            if (iv != null) {
                if (applicationStatus != null && !applicationStatus.isEmpty()) {
                    applicationDAO.updateApplicationStatus(iv.getApplicationId(), applicationStatus);
                }
                Applicant applicant = applicantDAO.getApplicantById(iv.getApplicantId());
                if (applicant != null) {
                    notificationDAO.createNotification(applicant.getUserId(), "Interview Completed & Evaluated",
                            "Your interview for '" + iv.getJobTitle() + "' has been evaluated by the hiring team.");
                }
            }
            ResponseHelper.sendSuccess(exchange, "Evaluation successfully saved!");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to save evaluation.");
        }
    }

    private int getOrResolveApplicantId(SessionManager.UserSession session) {
        if (session.getCandidateId() != null && session.getCandidateId() > 0) {
            return session.getCandidateId();
        }
        Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
        if (c != null) {
            session.setCandidateId(c.getCandidateId());
            return c.getCandidateId();
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

    private Date parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        dateStr = dateStr.trim();
        try {
            return Date.valueOf(dateStr);
        } catch (Exception ignored) {}
        try {
            String[] parts = dateStr.split("[-/]");
            if (parts.length == 3) {
                if (parts[0].length() == 4) { // YYYY-MM-DD
                    return Date.valueOf(parts[0] + "-" + pad2(parts[1]) + "-" + pad2(parts[2]));
                } else if (parts[2].length() == 4) { // DD-MM-YYYY
                    return Date.valueOf(parts[2] + "-" + pad2(parts[1]) + "-" + pad2(parts[0]));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String pad2(String s) {
        return s.length() == 1 ? "0" + s : s;
    }
}
