package com.recruitment.dao;

import com.recruitment.model.Recruiter;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Access Object for Recruiter profiles and recruiter dashboard metrics.
 */
public class RecruiterDAO {

    /**
     * Creates a new Recruiter profile.
     */
    public boolean createRecruiter(Recruiter r) {
        String sql = "INSERT INTO recruiters (user_id, recruiter_name, company_name, company_description, phone, country) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setString(2, r.getRecruiterName());
            ps.setString(3, r.getCompanyName());
            ps.setString(4, r.getCompanyDescription());
            ps.setString(5, r.getPhone());
            ps.setString(6, r.getCountry());

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
     * Gets recruiter profile by user_id.
     */
    public Recruiter getRecruiterByUserId(int userId) {
        String sql = "SELECT r.*, u.email FROM recruiters r JOIN users u ON r.user_id = u.user_id WHERE r.user_id = ?";
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
     * Gets recruiter profile by recruiter_id.
     */
    public Recruiter getRecruiterById(int recruiterId) {
        String sql = "SELECT r.*, u.email FROM recruiters r JOIN users u ON r.user_id = u.user_id WHERE r.recruiter_id = ?";
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
        String sql = "UPDATE recruiters SET recruiter_name = ?, company_name = ?, company_description = ?, phone = ?, country = ? " +
                "WHERE recruiter_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getRecruiterName());
            ps.setString(2, r.getCompanyName());
            ps.setString(3, r.getCompanyDescription());
            ps.setString(4, r.getPhone());
            ps.setString(5, r.getCountry());
            ps.setInt(6, r.getRecruiterId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RecruiterDAO.updateRecruiter] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves dashboard statistics for a recruiter:
     * - Total Jobs
     * - Active Jobs
     * - Total Applications
     * - Shortlisted Candidates
     * - Selected Candidates
     */
    public Map<String, Object> getRecruiterStats(int recruiterId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", 0);
        stats.put("activeJobs", 0);
        stats.put("totalApplications", 0);
        stats.put("shortlistedCandidates", 0);
        stats.put("selectedCandidates", 0);
        stats.put("interviewsScheduled", 0);

        String sql = "SELECT " +
                "  COUNT(DISTINCT j.job_id) AS total_jobs, " +
                "  SUM(CASE WHEN j.status = 'Active' THEN 1 ELSE 0 END) AS active_jobs, " +
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
        r.setRecruiterName(rs.getString("recruiter_name"));
        r.setCompanyName(rs.getString("company_name"));
        r.setCompanyDescription(rs.getString("company_description"));
        r.setPhone(rs.getString("phone"));
        r.setCountry(rs.getString("country"));
        r.setEmail(rs.getString("email"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}
