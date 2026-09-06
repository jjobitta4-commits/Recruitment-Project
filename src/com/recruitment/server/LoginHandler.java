package com.recruitment.server;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.dao.UserDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Candidate;
import com.recruitment.model.Recruiter;
import com.recruitment.model.User;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP Handler for User Authentication (Login, Logout, Session Verification).
 */
public class LoginHandler implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method)) {
                if (path.endsWith("/logout")) {
                    handleLogout(exchange);
                } else {
                    handleLogin(exchange);
                }
            } else if ("GET".equals(method)) {
                handleCheckSession(exchange);
            } else {
                ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("[LoginHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal server error occurred during authentication.");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        String email = JSONUtil.getString(data, "email", "").trim();
        String password = JSONUtil.getString(data, "password", "").trim();

        if (email.isEmpty() || password.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Email and password are required.");
            return;
        }

        User user = userDAO.loginUser(email, password);
        if (user == null) {
            ResponseHelper.sendError(exchange, 401, "Invalid email or password.");
            return;
        }

        Integer applicantId = null;
        Integer recruiterId = null;
        Integer candidateId = null;
        String name = user.getEmail();

        if ("applicant".equalsIgnoreCase(user.getRole()) || "candidate".equalsIgnoreCase(user.getRole())) {
            Applicant ap = applicantDAO.getApplicantByUserId(user.getUserId());
            if (ap != null) {
                applicantId = ap.getApplicantId();
                name = ap.getFullName();
            }
            Candidate c = candidateDAO.getCandidateByUserId(user.getUserId());
            if (c != null) {
                candidateId = c.getCandidateId();
                if (name == null || name.equals(user.getEmail())) {
                    name = c.getFullName();
                }
            }
        } else if ("recruiter".equalsIgnoreCase(user.getRole())) {
            Recruiter rc = recruiterDAO.getRecruiterByUserId(user.getUserId());
            if (rc != null) {
                recruiterId = rc.getRecruiterId();
                name = rc.getRecruiterName();
            }
        } else if ("admin".equalsIgnoreCase(user.getRole())) {
            name = "System Administrator";
        }

        // Generate session
        SessionManager.UserSession session = SessionManager.createSession(
                user.getUserId(), user.getEmail(), user.getRole(), applicantId, recruiterId, candidateId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", session.getToken());
        result.put("userId", user.getUserId());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("name", name);
        result.put("isVerified", user.isVerified());
        result.put("applicantId", applicantId);
        result.put("candidateId", candidateId);
        result.put("recruiterId", recruiterId);

        ResponseHelper.sendSuccess(exchange, "Login successful", result);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session != null) {
            SessionManager.removeSession(session.getToken());
        }
        ResponseHelper.sendSuccess(exchange, "Logged out successfully");
    }

    private void handleCheckSession(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "No active session.");
            return;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", session.getToken());
        result.put("userId", session.getUserId());
        result.put("email", session.getEmail());
        result.put("role", session.getRole());
        result.put("applicantId", session.getApplicantId());
        result.put("candidateId", session.getCandidateId());
        result.put("recruiterId", session.getRecruiterId());

        ResponseHelper.sendSuccess(exchange, "Session valid", result);
    }
}
