package com.recruitment.server;

import com.recruitment.dao.*;
import com.recruitment.model.Applicant;
import com.recruitment.model.Application;
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

        Interview iv = new Interview();
        iv.setApplicationId(applicationId);
        iv.setApplicantId(app.getApplicantId());
        iv.setJobId(app.getJobId());
        try {
            iv.setInterviewDate(Date.valueOf(dateStr.trim()));
        } catch (Exception e) {
            ResponseHelper.sendError(exchange, 400, "Invalid date format. Expected YYYY-MM-DD.");
            return;
        }
        iv.setInterviewTime(timeStr);
        iv.setInterviewType(type);
        iv.setMeetingLink(meetingLink);
        iv.setStatus("Scheduled");
        iv.setNotes(notes);

        boolean success = interviewDAO.scheduleInterview(iv);
        if (!success) {
            ResponseHelper.sendError(exchange, 500, "Failed to schedule interview.");
            return;
        }

        // Update application status to "Interview Scheduled"
        applicationDAO.updateApplicationStatus(applicationId, "Interview Scheduled");

        // Notify applicant
        Applicant applicant = applicantDAO.getApplicantById(app.getApplicantId());
        if (applicant != null) {
            String msg = String.format("An %s interview has been scheduled for '%s' at '%s' on %s at %s. Link/Location: %s",
                    type, app.getJobTitle(), app.getCompany(), dateStr, timeStr,
                    (meetingLink.isEmpty() ? "See interview details in portal" : meetingLink));
            notificationDAO.createNotification(applicant.getUserId(), "Interview Scheduled", msg);
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
