package com.recruitment.service;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.EmailVerificationDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.dao.UserDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Recruiter;
import com.recruitment.model.User;
import com.recruitment.util.EmailService;
import com.recruitment.util.SessionManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authentication and User Account Management Service.
 * Implements Controller -> Service -> DAO -> JDBC -> MySQL architectural layer.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final EmailVerificationDAO emailVerificationDAO = new EmailVerificationDAO();
    private final SecurityService securityService = new SecurityService();

    /**
     * Authenticates a user by email and password.
     */
    public Map<String, Object> authenticate(String email, String password, String ipAddress) {
        if (email == null || password == null) return null;
        String cleanEmail = email.trim().toLowerCase();

        User user = userDAO.loginUser(cleanEmail, password);
        if (user == null) {
            securityService.logSecurityEvent(null, "LOGIN_FAILED", "USER", null, "Failed login attempt for: " + cleanEmail, ipAddress);
            return null;
        }

        Integer applicantId = null;
        Integer recruiterId = null;
        String name = user.getEmail();

        if ("applicant".equalsIgnoreCase(user.getRole()) || "candidate".equalsIgnoreCase(user.getRole())) {
            Applicant ap = applicantDAO.getApplicantByUserId(user.getUserId());
            if (ap != null) {
                applicantId = ap.getApplicantId();
                name = ap.getFullName();
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

        SessionManager.UserSession session = SessionManager.createSession(
                user.getUserId(), user.getEmail(), user.getRole(), applicantId, recruiterId);

        securityService.logSecurityEvent(user.getUserId(), "LOGIN_SUCCESS", "USER", user.getUserId(), "User logged in with role: " + user.getRole(), ipAddress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", session.getToken());
        result.put("userId", user.getUserId());
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("name", name);
        result.put("isVerified", user.isVerified());
        result.put("applicantId", applicantId);
        result.put("recruiterId", recruiterId);

        return result;
    }

    /**
     * Sends a 6-digit OTP email verification code.
     */
    public boolean sendVerificationCode(String email) {
        String cleanEmail = email.trim().toLowerCase();
        String code = EmailService.generateCode();
        boolean saved = emailVerificationDAO.saveCode(cleanEmail, code, "REGISTRATION", 10);
        if (saved) {
            EmailService.sendVerificationEmail(cleanEmail, code);
            return true;
        }
        return false;
    }

    /**
     * Verifies the 6-digit OTP and marks the user account as verified.
     */
    public boolean verifyEmailCode(String email, String code) {
        String cleanEmail = email.trim().toLowerCase();
        boolean valid = emailVerificationDAO.verifyCode(cleanEmail, code, "REGISTRATION");
        if (valid) {
            userDAO.markUserVerified(cleanEmail);
            return true;
        }
        return false;
    }

    /**
     * Checks if an email is already taken.
     */
    public boolean isEmailRegistered(String email) {
        return userDAO.isEmailTaken(email);
    }
}
