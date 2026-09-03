package com.recruitment.dao;

import com.recruitment.model.User;
import com.recruitment.util.DBConnection;

import java.sql.*;

/**
 * Data Access Object for User operations (Login, Registration, Verification).
 */
public class UserDAO {

    /**
     * Authenticates a user by email and password.
     * @return User object if valid credentials, null otherwise.
     */
    public User loginUser(String email, String password) {
        String sql = "SELECT user_id, email, password, role, created_at FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO.loginUser] Error: " + e.getMessage());
        }
        return null;
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
     * Registers a new user account.
     * @return generated user_id on success, -1 on failure.
     */
    public int registerUser(String email, String password, String role) {
        String sql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, password);
            ps.setString(3, role.toLowerCase());
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
     * Retrieves a User by their user_id.
     */
    public User getUserById(int userId) {
        String sql = "SELECT user_id, email, password, role, created_at FROM users WHERE user_id = ?";
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

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
