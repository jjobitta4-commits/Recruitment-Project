package com.recruitment.dao;

import com.recruitment.util.DBConnection;

import java.sql.*;

public class EmailVerificationDAO {

    public boolean saveCode(String email, String code, String purpose, int validityMinutes) {
        String cleanEmail = email.trim().toLowerCase();
        String invalidateSql = "UPDATE email_verifications SET is_used = TRUE WHERE email = ? AND purpose = ? AND is_used = FALSE";
        String insertSql = "INSERT INTO email_verifications (email, code, purpose, expires_at, is_used) " +
                           "VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE), FALSE)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psInv = conn.prepareStatement(invalidateSql)) {
                    psInv.setString(1, cleanEmail);
                    psInv.setString(2, purpose);
                    psInv.executeUpdate();
                }

                try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                    psIns.setString(1, cleanEmail);
                    psIns.setString(2, code.trim());
                    psIns.setString(3, purpose);
                    psIns.setInt(4, validityMinutes);
                    psIns.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[EmailVerificationDAO.saveCode] Error: " + e.getMessage());
            return false;
        }
    }

    public boolean verifyCode(String email, String code, String purpose) {
        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();

        String selectSql = "SELECT verification_id FROM email_verifications " +
                           "WHERE email = ? AND code = ? AND purpose = ? AND is_used = FALSE AND expires_at > NOW() " +
                           "ORDER BY verification_id DESC LIMIT 1";

        String markUsedSql = "UPDATE email_verifications SET is_used = TRUE WHERE verification_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, cleanEmail);
            ps.setString(2, cleanCode);
            ps.setString(3, purpose);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("verification_id");
                    try (PreparedStatement psMark = conn.prepareStatement(markUsedSql)) {
                        psMark.setInt(1, id);
                        psMark.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailVerificationDAO.verifyCode] Error: " + e.getMessage());
        }
        return false;
    }

    public String getLastActiveCode(String email, String purpose) {
        String sql = "SELECT code FROM email_verifications " +
                     "WHERE email = ? AND purpose = ? AND is_used = FALSE AND expires_at > NOW() " +
                     "ORDER BY verification_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, purpose);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code");
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailVerificationDAO.getLastActiveCode] Error: " + e.getMessage());
        }
        return null;
    }

    public int getResendCooldownSeconds(String email, String purpose, int cooldownSeconds) {
        String sql = "SELECT TIMESTAMPDIFF(SECOND, created_at, NOW()) AS elapsed FROM email_verifications " +
                     "WHERE email = ? AND purpose = ? ORDER BY verification_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, purpose);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int elapsed = rs.getInt("elapsed");
                    int remaining = cooldownSeconds - elapsed;
                    return Math.max(0, remaining);
                }
            }
        } catch (SQLException ignored) {}
        return 0;
    }
}
