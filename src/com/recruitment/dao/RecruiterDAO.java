package com.recruitment.dao;

import com.recruitment.model.Recruiter;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Access Object for Recruiter profiles and recruiter dashboard metrics.
 * Fully aligned with normalized 'recruiters' and 'companies' tables.
 */
public class RecruiterDAO {

    /**
     * Creates a new Recruiter profile.
     */
    public boolean createRecruiter(Recruiter r) {
        String sql = "INSERT INTO recruiters (user_id, company_id, name, designation, phone) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            if (r.getCompanyId() > 0) {
                ps.setInt(2, r.getCompanyId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, r.getRecruiterName() != null ? r.getRecruiterName() : "Recruiter");
            ps.setString(4, r.getDesignation() != null ? r.getDesignation() : "Talent Acquisition");
            ps.setString(5, r.getPhone());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.setRecruiterId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.createRecruiter] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets recruiter profile by user_id with company details.
     */
    public Recruiter getRecruiterByUserId(int userId) {
        String sql = "SELECT r.*, u.email, c.name AS company_name, c.description AS comp_desc " +
                "FROM recruiters r " +
                "JOIN users u ON r.user_id = u.user_id " +
                "LEFT JOIN companies c ON r.company_id = c.company_id " +
                "WHERE r.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecruiter(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.getRecruiterByUserId] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets recruiter profile by recruiter_id with company details.
     */
    public Recruiter getRecruiterById(int recruiterId) {
        String sql = "SELECT r.*, u.email, c.name AS company_name, c.description AS comp_desc " +
                "FROM recruiters r " +
                "JOIN users u ON r.user_id = u.user_id " +
                "LEFT JOIN companies c ON r.company_id = c.company_id " +
                "WHERE r.recruiter_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecruiter(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.getRecruiterById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates recruiter profile.
     */
    public boolean updateRecruiter(Recruiter r) {
        String sql = "UPDATE recruiters SET name = ?, designation = ?, phone = ?, company_id = ? " +
                "WHERE recruiter_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getRecruiterName());
            ps.setString(2, r.getDesignation());
            ps.setString(3, r.getPhone());
            if (r.getCompanyId() > 0) {
                ps.setInt(4, r.getCompanyId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, r.getRecruiterId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.updateRecruiter] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Associates a recruiter with a company.
     */
    public boolean setRecruiterCompany(int recruiterId, int companyId) {
        String sql = "UPDATE recruiters SET company_id = ? WHERE recruiter_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, recruiterId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.setRecruiterCompany] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves dashboard statistics for a recruiter:
     * - Total Jobs
     * - Active Jobs
     * - Pending Jobs
     * - Total Applications
     * - Shortlisted Candidates
     * - Selected Candidates
     */
    public Map<String, Object> getRecruiterStats(int recruiterId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", 0);
        stats.put("activeJobs", 0);
        stats.put("pendingJobs", 0);
        stats.put("totalApplications", 0);
        stats.put("shortlistedCandidates", 0);
        stats.put("selectedCandidates", 0);
        stats.put("interviewsScheduled", 0);

        String sql = "SELECT " +
                "  COUNT(DISTINCT j.job_id) AS total_jobs, " +
                "  SUM(CASE WHEN j.status = 'Active' THEN 1 ELSE 0 END) AS active_jobs, " +
                "  SUM(CASE WHEN j.approval_status = 'pending' THEN 1 ELSE 0 END) AS pending_jobs, " +
                "  COUNT(a.application_id) AS total_apps, " +
                "  SUM(CASE WHEN a.status = 'Shortlisted' THEN 1 ELSE 0 END) AS shortlisted_apps, " +
                "  SUM(CASE WHEN a.status = 'Selected' THEN 1 ELSE 0 END) AS selected_apps, " +
                "  SUM(CASE WHEN a.status = 'Interview Scheduled' THEN 1 ELSE 0 END) AS interview_apps " +
                "FROM jobs j " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.recruiter_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalJobs", rs.getInt("total_jobs"));
                    stats.put("activeJobs", rs.getInt("active_jobs"));
                    stats.put("pendingJobs", rs.getInt("pending_jobs"));
                    stats.put("totalApplications", rs.getInt("total_apps"));
                    stats.put("shortlistedCandidates", rs.getInt("shortlisted_apps"));
                    stats.put("selectedCandidates", rs.getInt("selected_apps"));
                    stats.put("interviewsScheduled", rs.getInt("interview_apps"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.getRecruiterStats] Error: " + e.getMessage());
        }

        return stats;
    }

    private Recruiter mapRecruiter(ResultSet rs) throws SQLException {
        Recruiter r = new Recruiter();
        r.setRecruiterId(rs.getInt("recruiter_id"));
        r.setUserId(rs.getInt("user_id"));
        try { r.setCompanyId(rs.getInt("company_id")); } catch (SQLException ignored) {}
        try { r.setRecruiterName(rs.getString("name")); } catch (SQLException e) {
            try { r.setRecruiterName(rs.getString("recruiter_name")); } catch (SQLException ignored) {}
        }
        try { r.setDesignation(rs.getString("designation")); } catch (SQLException ignored) {}
        try { r.setCompanyName(rs.getString("company_name")); } catch (SQLException ignored) {}
        try { r.setCompanyDescription(rs.getString("comp_desc")); } catch (SQLException ignored) {}
        r.setPhone(rs.getString("phone"));
        try { r.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        try { r.setCreatedAt(rs.getTimestamp("created_at")); } catch (SQLException ignored) {}
        return r;
    }
}
