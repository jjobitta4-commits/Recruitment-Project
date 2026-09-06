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
        String sql = "INSERT INTO interviews (application_id, recruiter_id, candidate_id, interview_date, " +
                "interview_time, interview_type, meeting_link, status, feedback) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, iv.getApplicationId());
            ps.setInt(2, iv.getRecruiterId());
            ps.setInt(3, iv.getCandidateId());
            ps.setDate(4, iv.getInterviewDate());
            ps.setString(5, iv.getInterviewTime());
            ps.setString(6, (iv.getInterviewType() != null) ? iv.getInterviewType() : "Online");
            ps.setString(7, iv.getMeetingLink());
            ps.setString(8, (iv.getStatus() != null) ? iv.getStatus() : "Scheduled");
            ps.setString(9, iv.getFeedback() != null ? iv.getFeedback() : iv.getNotes());

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
                "  c.full_name AS candidate_name, u.email AS candidate_email, " +
                "  a.job_id, j.title AS job_title, COALESCE(comp.name, 'Company') AS company_name " +
                "FROM interviews iv " +
                "JOIN candidates c ON iv.candidate_id = c.candidate_id " +
                "JOIN users u ON c.user_id = u.user_id " +
                "JOIN applications a ON iv.application_id = a.application_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "LEFT JOIN companies comp ON j.company_id = comp.company_id " +
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
     * Retrieves all interviews for a given applicant or candidate.
     */
    public List<Interview> getInterviewsByApplicant(int candidateId) {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT iv.*, " +
                "  c.full_name AS candidate_name, u.email AS candidate_email, " +
                "  a.job_id, j.title AS job_title, COALESCE(comp.name, 'Company') AS company_name " +
                "FROM interviews iv " +
                "JOIN candidates c ON iv.candidate_id = c.candidate_id " +
                "JOIN users u ON c.user_id = u.user_id " +
                "JOIN applications a ON iv.application_id = a.application_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "LEFT JOIN companies comp ON j.company_id = comp.company_id " +
                "WHERE iv.candidate_id = ? " +
                "   OR c.user_id = ? " +
                "   OR c.user_id = (SELECT user_id FROM applicants WHERE applicant_id = ?) " +
                "ORDER BY iv.interview_date ASC, iv.interview_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.setInt(2, candidateId);
            ps.setInt(3, candidateId);
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
                "  c.full_name AS candidate_name, u.email AS candidate_email, " +
                "  a.job_id, j.title AS job_title, COALESCE(comp.name, 'Company') AS company_name " +
                "FROM interviews iv " +
                "JOIN candidates c ON iv.candidate_id = c.candidate_id " +
                "JOIN users u ON c.user_id = u.user_id " +
                "JOIN applications a ON iv.application_id = a.application_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "LEFT JOIN companies comp ON j.company_id = comp.company_id " +
                "WHERE iv.recruiter_id = ? OR j.recruiter_id = ? " +
                "ORDER BY iv.interview_date ASC, iv.interview_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            ps.setInt(2, recruiterId);
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
     * Updates an interview's status.
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

    /**
     * Records candidate technical evaluation scorecard from the virtual interview room.
     */
    public boolean saveEvaluation(int interviewId, int rating, String feedback, String status) {
        String sql = "UPDATE interviews SET rating = ?, evaluation_feedback = ?, status = ? WHERE interview_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating);
            ps.setString(2, feedback);
            ps.setString(3, (status != null && !status.isEmpty()) ? status : "Completed");
            ps.setInt(4, interviewId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[InterviewDAO.saveEvaluation] Error: " + e.getMessage());
        }
        return false;
    }

    private Interview mapInterview(ResultSet rs) throws SQLException {
        Interview iv = new Interview();
        iv.setInterviewId(rs.getInt("interview_id"));
        iv.setApplicationId(rs.getInt("application_id"));
        iv.setRecruiterId(rs.getInt("recruiter_id"));
        iv.setCandidateId(rs.getInt("candidate_id"));
        iv.setApplicantId(rs.getInt("candidate_id"));
        try { iv.setJobId(rs.getInt("job_id")); } catch (SQLException ignored) {}
        iv.setInterviewDate(rs.getDate("interview_date"));
        iv.setInterviewTime(rs.getString("interview_time"));
        iv.setInterviewType(rs.getString("interview_type"));
        iv.setMeetingLink(rs.getString("meeting_link"));
        iv.setStatus(rs.getString("status"));
        iv.setFeedback(rs.getString("feedback"));
        iv.setNotes(rs.getString("feedback"));
        try { iv.setRating(rs.getInt("rating")); } catch (SQLException ignored) {}
        try { iv.setEvaluationFeedback(rs.getString("evaluation_feedback")); } catch (SQLException ignored) {}
        try { iv.setTechnicalScore(rs.getInt("technical_score")); } catch (SQLException ignored) {}
        try { iv.setCommunicationScore(rs.getInt("communication_score")); } catch (SQLException ignored) {}
        try { iv.setProblemSolvingScore(rs.getInt("problem_solving_score")); } catch (SQLException ignored) {}
        try { iv.setOverallScore(rs.getInt("overall_score")); } catch (SQLException ignored) {}
        iv.setCreatedAt(rs.getTimestamp("created_at"));

        iv.setApplicantName(rs.getString("candidate_name"));
        iv.setCandidateName(rs.getString("candidate_name"));
        iv.setApplicantEmail(rs.getString("candidate_email"));
        iv.setCandidateEmail(rs.getString("candidate_email"));
        try { iv.setJobTitle(rs.getString("job_title")); } catch (SQLException ignored) {}
        try { iv.setCompany(rs.getString("company_name")); } catch (SQLException ignored) {}

        return iv;
    }
}
