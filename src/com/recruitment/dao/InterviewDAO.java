package com.recruitment.dao;

import com.recruitment.model.Interview;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Interview scheduling and tracking.
 */
public class InterviewDAO {

    /**
     * Schedules a new interview round.
     */
    public boolean scheduleInterview(Interview iv) {
        String sql = "INSERT INTO interviews (application_id, applicant_id, job_id, interview_date, " +
                "interview_time, interview_type, meeting_link, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, iv.getApplicationId());
            ps.setInt(2, iv.getApplicantId());
            ps.setInt(3, iv.getJobId());
            ps.setDate(4, iv.getInterviewDate());
            ps.setString(5, iv.getInterviewTime());
            ps.setString(6, (iv.getInterviewType() != null) ? iv.getInterviewType() : "Online");
            ps.setString(7, iv.getMeetingLink());
            ps.setString(8, (iv.getStatus() != null) ? iv.getStatus() : "Scheduled");
            ps.setString(9, iv.getNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        iv.setInterviewId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.scheduleInterview] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets interview by ID.
     */
    public Interview getInterviewById(int interviewId) {
        String sql = "SELECT iv.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, " +
                "  j.title AS job_title, j.company AS company " +
                "FROM interviews iv " +
                "JOIN applicants ap ON iv.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON iv.job_id = j.job_id " +
                "WHERE iv.interview_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, interviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapInterview(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.getInterviewById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all interviews for a given applicant.
     */
    public List<Interview> getInterviewsByApplicant(int applicantId) {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT iv.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, " +
                "  j.title AS job_title, j.company AS company " +
                "FROM interviews iv " +
                "JOIN applicants ap ON iv.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON iv.job_id = j.job_id " +
                "WHERE iv.applicant_id = ? ORDER BY iv.interview_date ASC, iv.interview_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.getInterviewsByApplicant] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all interviews for jobs owned by a recruiter.
     */
    public List<Interview> getInterviewsByRecruiter(int recruiterId) {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT iv.*, " +
                "  ap.full_name AS applicant_name, u.email AS applicant_email, " +
                "  j.title AS job_title, j.company AS company " +
                "FROM interviews iv " +
                "JOIN applicants ap ON iv.applicant_id = ap.applicant_id " +
                "JOIN users u ON ap.user_id = u.user_id " +
                "JOIN jobs j ON iv.job_id = j.job_id " +
                "WHERE j.recruiter_id = ? ORDER BY iv.interview_date ASC, iv.interview_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapInterview(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.getInterviewsByRecruiter] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates status of an interview (Scheduled, Completed, Cancelled).
     */
    public boolean updateInterviewStatus(int interviewId, String status) {
        String sql = "UPDATE interviews SET status = ? WHERE interview_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, interviewId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.updateInterviewStatus] Error: " + e.getMessage());
        }
        return false;
    }

    private Interview mapInterview(ResultSet rs) throws SQLException {
        Interview iv = new Interview();
        iv.setInterviewId(rs.getInt("interview_id"));
        iv.setApplicationId(rs.getInt("application_id"));
        iv.setApplicantId(rs.getInt("applicant_id"));
        iv.setJobId(rs.getInt("job_id"));
        iv.setInterviewDate(rs.getDate("interview_date"));
        iv.setInterviewTime(rs.getString("interview_time"));
        iv.setInterviewType(rs.getString("interview_type"));
        iv.setMeetingLink(rs.getString("meeting_link"));
        iv.setStatus(rs.getString("status"));
        iv.setNotes(rs.getString("notes"));
        iv.setCreatedAt(rs.getTimestamp("created_at"));

        iv.setApplicantName(rs.getString("applicant_name"));
        iv.setApplicantEmail(rs.getString("applicant_email"));
        iv.setJobTitle(rs.getString("job_title"));
        iv.setCompany(rs.getString("company"));

        return iv;
    }
}
