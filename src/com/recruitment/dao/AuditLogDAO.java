package com.recruitment.dao;

import com.recruitment.model.AuditLog;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for tracking system security events and transactional audit logs.
 */
public class AuditLogDAO {

    /**
     * Records a new audit log event in the database.
     */
    public boolean log(Integer userId, String action, String entityType, Integer entityId, String details, String ipAddress) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId != null && userId > 0) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId != null && entityId > 0) ps.setInt(4, entityId); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, details);
            ps.setString(6, ipAddress != null ? ipAddress : "127.0.0.1");

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AuditLogDAO.log] Error recording audit event: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the latest audit log records for the system administrator dashboard.
     */
    public List<AuditLog> getRecentLogs(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT log_id, user_id, action, entity_type, entity_id, details, ip_address, created_at " +
                     "FROM audit_logs ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setLogId(rs.getInt("log_id"));
                    int uid = rs.getInt("user_id");
                    log.setUserId(rs.wasNull() ? null : uid);
                    log.setAction(rs.getString("action"));
                    log.setEntityType(rs.getString("entity_type"));
                    int eid = rs.getInt("entity_id");
                    log.setEntityId(rs.wasNull() ? null : eid);
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuditLogDAO.getRecentLogs] Error: " + e.getMessage());
        }
        return logs;
    }
}
