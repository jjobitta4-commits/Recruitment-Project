package com.recruitment.util;

import com.sun.net.httpserver.HttpExchange;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Session Manager for Core Java HTTP Server.
 * <p>
 * Manages user sessions without external servlet/Tomcat dependencies.
 * Each authenticated user is assigned a unique session token sent back in the HTTP response.
 * Subsequent client requests provide this token via the 'X-Session-Token' or 'Authorization: Bearer <token>' header.
 * </p>
 */
public class SessionManager {

    /**
     * Data holder representing an active authenticated user session.
     */
    public static class UserSession {
        private final String token;
        private final int userId;
        private final String email;
        private final String role; // "applicant" or "recruiter"
        private Integer applicantId;
        private Integer recruiterId;
        private Integer candidateId;
        private final long createdAt;
        private long lastAccessed;

        public UserSession(String token, int userId, String email, String role, Integer applicantId, Integer recruiterId) {
            this(token, userId, email, role, applicantId, recruiterId, applicantId);
        }

        public UserSession(String token, int userId, String email, String role, Integer applicantId, Integer recruiterId, Integer candidateId) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.applicantId = applicantId;
            this.recruiterId = recruiterId;
            this.candidateId = candidateId != null ? candidateId : applicantId;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = this.createdAt;
        }

        public String getToken() { return token; }
        public int getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public Integer getApplicantId() { return applicantId; }
        public void setApplicantId(Integer applicantId) { this.applicantId = applicantId; }
        public Integer getCandidateId() { return candidateId != null ? candidateId : applicantId; }
        public void setCandidateId(Integer candidateId) { this.candidateId = candidateId; }
        public Integer getRecruiterId() { return recruiterId; }
        public void setRecruiterId(Integer recruiterId) { this.recruiterId = recruiterId; }
        public long getCreatedAt() { return createdAt; }
        public long getLastAccessed() { return lastAccessed; }
        public void touch() { this.lastAccessed = System.currentTimeMillis(); }

        public boolean isApplicant() { return "applicant".equalsIgnoreCase(role) || "candidate".equalsIgnoreCase(role); }
        public boolean isCandidate() { return "candidate".equalsIgnoreCase(role) || "applicant".equalsIgnoreCase(role); }
        public boolean isRecruiter() { return "recruiter".equalsIgnoreCase(role); }
        public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
    }

    // In-memory concurrent registry of active sessions: Token -> UserSession
    private static final Map<String, UserSession> SESSIONS = new ConcurrentHashMap<>();

    // Default session timeout: 24 hours in milliseconds
    private static final long SESSION_TIMEOUT_MS = 24L * 60 * 60 * 1000;

    /**
     * Creates and registers a new session token for an authenticated user.
     */
    public static UserSession createSession(int userId, String email, String role, Integer applicantId, Integer recruiterId) {
        return createSession(userId, email, role, applicantId, recruiterId, applicantId);
    }

    public static UserSession createSession(int userId, String email, String role, Integer applicantId, Integer recruiterId, Integer candidateId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession(token, userId, email, role, applicantId, recruiterId, candidateId);
        SESSIONS.put(token, session);
        return session;
    }

    /**
     * Retrieves an active session by token.
     * @return UserSession if valid, null if expired or not found.
     */
    public static UserSession getSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        token = token.trim();
        UserSession session = SESSIONS.get(token);
        if (session != null) {
            if (System.currentTimeMillis() - session.getLastAccessed() > SESSION_TIMEOUT_MS) {
                SESSIONS.remove(token);
                return null;
            }
            session.touch();
            return session;
        }
        return null;
    }

    /**
     * Extracts and validates session from HttpExchange headers (X-Session-Token, Authorization, or Cookie).
     */
    public static UserSession getSessionFromExchange(HttpExchange exchange) {
        // 1. Check custom X-Session-Token header
        String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
        if (token != null && !token.trim().isEmpty()) {
            return getSession(token);
        }

        // 2. Check Authorization: Bearer <token>
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            token = authHeader.substring(7).trim();
            return getSession(token);
        }

        // 3. Check Cookie: session_token=<token>
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "session_token".equalsIgnoreCase(parts[0].trim())) {
                    return getSession(parts[1].trim());
                }
            }
        }

        return null;
    }

    /**
     * Invalidates and removes a session on logout.
     */
    public static void removeSession(String token) {
        if (token != null) {
            SESSIONS.remove(token.trim());
        }
    }
}
