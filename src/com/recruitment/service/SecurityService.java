package com.recruitment.service;

import com.recruitment.dao.AuditLogDAO;
import com.recruitment.util.PasswordUtil;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service layer handling password cryptography, token generation, role verification, and audit dispatching.
 */
public class SecurityService {

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 32-byte cryptographically secure random session token.
     */
    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Verifies password strength. Requires at least 6 characters.
     */
    public boolean isPasswordStrong(String password) {
        return password != null && password.trim().length() >= 6;
    }

    /**
     * Hashes a password with a unique salt using SHA-256.
     */
    public String hashPassword(String password, String salt) {
        return PasswordUtil.hashPassword(password, salt);
    }

    /**
     * Generates a unique 16-byte random salt in hex.
     */
    public String generateSalt() {
        return PasswordUtil.generateSalt();
    }

    /**
     * Verifies a plain text password against stored salt and SHA-256 hash.
     */
    public boolean verifyPassword(String password, String salt, String storedHash) {
        return PasswordUtil.verifyPassword(password, salt, storedHash);
    }

    /**
     * Checks whether a user's role matches any of the allowed roles.
     */
    public boolean hasRole(String userRole, String... allowedRoles) {
        if (userRole == null) return false;
        for (String role : allowedRoles) {
            if (userRole.equalsIgnoreCase(role.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs a security audit event.
     */
    public void logSecurityEvent(Integer userId, String action, String entityType, Integer entityId, String details, String ipAddress) {
        auditLogDAO.log(userId, action, entityType, entityId, details, ipAddress);
    }
}
