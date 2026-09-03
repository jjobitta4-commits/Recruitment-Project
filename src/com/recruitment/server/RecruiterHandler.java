package com.recruitment.server;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Recruiter;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Recruiter Dashboard Metrics and Candidate Search.
 */
public class RecruiterHandler implements HttpHandler {

    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Recruiter authorization required.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        if (recruiterId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.endsWith("/stats") && "GET".equals(method)) {
                Map<String, Object> stats = recruiterDAO.getRecruiterStats(recruiterId);
                ResponseHelper.sendSuccess(exchange, "Recruiter dashboard statistics retrieved", stats);

            } else if (path.endsWith("/profile") && "GET".equals(method)) {
                Recruiter r = recruiterDAO.getRecruiterById(recruiterId);
                ResponseHelper.sendSuccess(exchange, "Recruiter profile retrieved", r);

            } else if (path.endsWith("/profile") && "PUT".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Recruiter r = recruiterDAO.getRecruiterById(recruiterId);
                if (r == null) {
                    ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
                    return;
                }

                r.setRecruiterName(JSONUtil.getString(data, "recruiterName", r.getRecruiterName()));
                r.setCompanyName(JSONUtil.getString(data, "companyName", r.getCompanyName()));
                r.setCompanyDescription(JSONUtil.getString(data, "companyDescription", r.getCompanyDescription()));
                r.setPhone(JSONUtil.getString(data, "phone", r.getPhone()));
                r.setCountry(JSONUtil.getString(data, "country", r.getCountry()));

                boolean success = recruiterDAO.updateRecruiter(r);
                if (success) {
                    ResponseHelper.sendSuccess(exchange, "Profile updated successfully", r);
                } else {
                    ResponseHelper.sendError(exchange, 500, "Failed to update profile.");
                }

            } else if (path.endsWith("/candidates") && "GET".equals(method)) {
                Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
                if (query.containsKey("id")) {
                    try {
                        int applicantId = Integer.parseInt(query.get("id"));
                        Applicant ap = applicantDAO.getApplicantById(applicantId);
                        if (ap != null) {
                            ResponseHelper.sendSuccess(exchange, "Candidate details retrieved", ap);
                        } else {
                            ResponseHelper.sendError(exchange, 404, "Candidate not found.");
                        }
                    } catch (NumberFormatException e) {
                        ResponseHelper.sendError(exchange, 400, "Invalid candidate ID.");
                    }
                    return;
                }

                List<Applicant> candidates = applicantDAO.getAllApplicants();
                ResponseHelper.sendSuccess(exchange, "Candidates directory retrieved", candidates);

            } else {
                ResponseHelper.sendError(exchange, 404, "Unknown recruiter API endpoint.");
            }
        } catch (Exception e) {
            System.err.println("[RecruiterHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal error in recruiter operations.");
        }
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
