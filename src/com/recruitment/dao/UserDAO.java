package com.recruitment.dao;

import com.recruitment.model.User;
import com.recruitment.util.DBConnection;
import com.recruitment.util.PasswordUtil;

import java.sql.*;

/**
 * Data Access Object for User operations (Authentication, Registration, Profile, Password Security).
 * Employs PreparedStatement exclusively and supports salted SHA-256 password hashing.
 */
public class UserDAO {

    /**
     * Authenticates a user by email and raw password.
     * Verifies against salted SHA-256 hash or plaintext fallback for legacy accounts.
     * @return User object if valid, null otherwise.
     */
    public User loginUser(String email, String password) {
        if (email == null || password == null) return null;
        String cleanEmail = email.trim().toLowerCase();

        String sql = "SELECT user_id, email, password_hash, salt, role, status, is_verified, created_at " +
                     "FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cleanEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");
                    String status = rs.getString("status");

                    if ("disabled".equalsIgnoreCase(status)) {
                        System.err.println("[UserDAO.loginUser] Account disabled: " + cleanEmail);
                        return null;
                    }

                    boolean authenticated = false;

                    // 1. Try salted hash verification
                    if (storedSalt != null && !storedSalt.isEmpty() && storedHash != null) {
                        authenticated = PasswordUtil.verifyPassword(password, storedSalt, storedHash);
                    }

                    // 2. Legacy fallback for plaintext migration
                    if (!authenticated && storedHash != null && storedHash.equals(password)) {
                        authenticated = true;
                        // Auto-upgrade password to salted SHA-256
                        upgradePasswordHash(rs.getInt("user_id"), password);
                    }

                    if (authenticated) {
                        User user = mapUser(rs);
                        updateLastLogin(user.getUserId());
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.loginUser] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers a new user account with a salted SHA-256 password hash.
     * @return generated user_id on success, -1 on failure.
     */
    public int registerUser(String email, String password, String role) {
        String cleanEmail = email.trim().toLowerCase();

        // Enforce admin exclusivity: Only admin@recruithub.com can ever have the 'admin' role
        if ("admin".equalsIgnoreCase(role) && !"admin@recruithub.com".equalsIgnoreCase(cleanEmail)) {
            System.err.println("[UserDAO.registerUser] Blocked unauthorized attempt to register admin role for email: " + cleanEmail);
            return -1;
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        String sql = "INSERT INTO users (email, password_hash, salt, role, status, is_verified) VALUES (?, ?, ?, ?, 'active', FALSE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cleanEmail);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, role.toLowerCase());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.registerUser] Error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Checks if an email is already registered in the system.
     */
    public boolean isEmailTaken(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.isEmailTaken] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves a User by their user_id.
     */
    public User getUserById(int userId) {
        String sql = "SELECT user_id, email, password_hash, salt, role, status, is_verified, created_at, last_login " +
                     "FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.getUserById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a User by their email.
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT user_id, email, password_hash, salt, role, status, is_verified, created_at " +
                     "FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.getUserByEmail] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Marks a user's email address as verified in the database.
     */
    public boolean markUserVerified(String email) {
        String sql = "UPDATE users SET is_verified = TRUE WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO.markUserVerified] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if a user's email has already been verified.
     */
    public boolean isUserVerified(String email) {
        String sql = "SELECT is_verified FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_verified");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.isUserVerified] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates the last login timestamp for a user.
     */
    public void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO.updateLastLogin] Error: " + e.getMessage());
        }
    }

    /**
     * Updates user password with salted SHA-256 hash.
     */
    public boolean updatePassword(int userId, String newPassword) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO.updatePassword] Error: " + e.getMessage());
        }
        return false;
    }

    private void upgradePasswordHash(int userId, String rawPassword) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            ps.executeUpdate();
            System.out.println("[UserDAO] Transparently upgraded password hash for userId: " + userId);
        } catch (SQLException ignored) {}
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setRole(rs.getString("role"));
        try {
            u.setStatus(rs.getString("status"));
        } catch (SQLException ignored) {}
        try {
            u.setVerified(rs.getBoolean("is_verified"));
        } catch (SQLException ignored) {}
        u.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            u.setLastLogin(rs.getTimestamp("last_login"));
        } catch (SQLException ignored) {}
        return u;
    }
}
