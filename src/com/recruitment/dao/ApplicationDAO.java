package com.recruitment.dao;

import com.recruitment.model.Application;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Job Applications.
 */
public class ApplicationDAO {

    /**
     * Checks if an applicant has already applied for a specific job.
     */
    public boolean hasApplied(int applicantId, int jobId) {
        String sql = "SELECT 1 FROM applications WHERE applicant_id = ? AND job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            ps.setInt(2, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.hasApplied] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Submits a new job application.
     */
    public boolean applyForJob(Application app) {
        String sql = "INSERT INTO applications (applicant_id, job_id, resume_path, cover_letter, status) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, app.getApplicantId());
            ps.setInt(2, app.getJobId());
            ps.setString(3, app.getResumePath());
            ps.setString(4, app.getCoverLetter());
            ps.setString(5, (app.getStatus() != null && !app.getStatus().isEmpty()) ? app.getStatus() : "Applied");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        app.setApplicationId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.applyForJob] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets an application by ID with joined applicant and job details.
     */
    public Application getApplicationById(int applicationId) {
        String sql = "SELECT a.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, ap.phone AS applicant_phone, " +
                "  ap.skills AS applicant_skills, ap.experience_years AS applicant_experience, ap.education AS applicant_education, " +
                "  j.title AS job_title, j.company AS job_company, j.location AS job_location, j.job_type AS job_type " +
                "FROM applications a " +
                "JOIN applicants ap ON a.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "WHERE a.application_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapApplication(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all applications submitted by an applicant.
     */
    public List<Application> getApplicationsByApplicant(int applicantId) {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, ap.phone AS applicant_phone, " +
                "  ap.skills AS applicant_skills, ap.experience_years AS applicant_experience, ap.education AS applicant_education, " +
                "  j.title AS job_title, j.company AS job_company, j.location AS job_location, j.job_type AS job_type " +
                "FROM applications a " +
                "JOIN applicants ap ON a.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "WHERE a.applicant_id = ? ORDER BY a.applied_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapApplication(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationsByApplicant] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all applications received for jobs posted by a specific recruiter.
     */
    public List<Application> getApplicationsByRecruiter(int recruiterId, Integer jobId, String statusFilter) {
        List<Application> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT a.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, ap.phone AS applicant_phone, " +
                "  ap.skills AS applicant_skills, ap.experience_years AS applicant_experience, ap.education AS applicant_education, " +
                "  j.title AS job_title, j.company AS job_company, j.location AS job_location, j.job_type AS job_type " +
                "FROM applications a " +
                "JOIN applicants ap ON a.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "WHERE j.recruiter_id = ? ");

        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        if (jobId != null && jobId > 0) {
            sql.append("AND a.job_id = ? ");
            params.add(jobId);
        }
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
            sql.append("AND a.status = ? ");
            params.add(statusFilter.trim());
        }

        sql.append("ORDER BY a.applied_date DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapApplication(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationsByRecruiter] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an application's review status.
     * Allowed: Applied, Under Review, Shortlisted, Interview Scheduled, Selected, Rejected
     */
    public boolean updateApplicationStatus(int applicationId, String status) {
        String sql = "UPDATE applications SET status = ?, updated_date = CURRENT_TIMESTAMP WHERE application_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.updateApplicationStatus] Error: " + e.getMessage());
        }
        return false;
    }

    private Application mapApplication(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setApplicationId(rs.getInt("application_id"));
        a.setApplicantId(rs.getInt("applicant_id"));
        a.setJobId(rs.getInt("job_id"));
        a.setResumePath(rs.getString("resume_path"));
        a.setCoverLetter(rs.getString("cover_letter"));
        a.setStatus(rs.getString("status"));
        a.setAppliedDate(rs.getTimestamp("applied_date"));
        a.setUpdatedDate(rs.getTimestamp("updated_date"));

        a.setApplicantName(rs.getString("applicant_name"));
        a.setApplicantEmail(rs.getString("applicant_email"));
        a.setApplicantPhone(rs.getString("applicant_phone"));
        a.setApplicantSkills(rs.getString("applicant_skills"));
        a.setApplicantExperience(rs.getInt("applicant_experience"));
        a.setApplicantEducation(rs.getString("applicant_education"));
        a.setJobTitle(rs.getString("job_title"));
        a.setCompany(rs.getString("job_company"));
        a.setJobLocation(rs.getString("job_location"));
        a.setJobType(rs.getString("job_type"));

        return a;
    }
}
