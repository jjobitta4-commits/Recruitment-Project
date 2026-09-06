package com.recruitment.server;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.EmailVerificationDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.dao.UserDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Recruiter;
import com.recruitment.model.User;
import com.recruitment.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmailVerificationHandler implements HttpHandler {

    private final EmailVerificationDAO verificationDAO = new EmailVerificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();

    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("POST".equals(method)) {
                if (path.endsWith("/verify")) {
                    handleVerify(exchange);
                } else if (path.endsWith("/resend")) {
                    handleSendOrResend(exchange, true);
                } else {
                    handleSendOrResend(exchange, false);
                }
            } else if ("GET".equals(method)) {
                handlePeek(exchange);
            } else {
                ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("[EmailVerificationHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal server error during email verification.");
        }
    }

    private void handleSendOrResend(HttpExchange exchange, boolean isResend) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        String email = JSONUtil.getString(data, "email", "").trim().toLowerCase();
        String purpose = JSONUtil.getString(data, "purpose", "REGISTRATION").trim().toUpperCase();

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            ResponseHelper.sendError(exchange, 400, "Please provide a valid email address.");
            return;
        }

        int remainingCooldown = verificationDAO.getResendCooldownSeconds(email, purpose, RESEND_COOLDOWN_SECONDS);
        if (remainingCooldown > 0) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("cooldownRemaining", remainingCooldown);
            resp.put("email", email);
            Map<String, Object> errPayload = new LinkedHashMap<>();
            errPayload.put("success", false);
            errPayload.put("message", "Please wait " + remainingCooldown + " seconds before requesting a new code.");
            errPayload.put("data", resp);
            ResponseHelper.sendJson(exchange, 429, errPayload);
            return;
        }

        String code = EmailService.generateCode();
        boolean saved = verificationDAO.saveCode(email, code, purpose, CODE_EXPIRATION_MINUTES);
        if (!saved) {
            ResponseHelper.sendError(exchange, 500, "Failed to generate verification code. Please try again.");
            return;
        }

        EmailService.sendVerificationEmail(email, code, purpose);

        Map<String, Object> respData = new LinkedHashMap<>();
        respData.put("email", email);
        respData.put("purpose", purpose);
        respData.put("cooldownSeconds", RESEND_COOLDOWN_SECONDS);
        respData.put("expiresInMinutes", CODE_EXPIRATION_MINUTES);
        respData.put("peekCode", code);

        String actionMsg = isResend ? "New verification code resent to " : "Verification code sent to ";
        ResponseHelper.sendSuccess(exchange, actionMsg + email + ". Check your inbox or terminal.", respData);
    }

    private void handleVerify(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        String email = JSONUtil.getString(data, "email", "").trim().toLowerCase();
        String code = JSONUtil.getString(data, "code", "").trim();
        String purpose = JSONUtil.getString(data, "purpose", "REGISTRATION").trim().toUpperCase();

        if (email.isEmpty() || code.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Email and verification code are required.");
            return;
        }

        boolean valid = verificationDAO.verifyCode(email, code, purpose);
        if (!valid) {
            ResponseHelper.sendError(exchange, 400, "Invalid or expired verification code. Please try again.");
            return;
        }

        userDAO.markUserVerified(email);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", email);
        result.put("verified", true);

        System.out.println("[EmailVerificationHandler] Email successfully verified: " + email);
        ResponseHelper.sendSuccess(exchange, "Email address verified successfully!", result);
    }

    private void handlePeek(HttpExchange exchange) throws IOException {
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String email = query.getOrDefault("email", "").trim().toLowerCase();
        String code = EmailService.getRecentCode(email);
        if (code == null) {
            code = verificationDAO.getLastActiveCode(email, "REGISTRATION");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", email);
        result.put("code", code);
        ResponseHelper.sendSuccess(exchange, "Verification status peek", result);
    }
}
