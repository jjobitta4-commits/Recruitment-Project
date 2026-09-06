package com.recruitment.dao;

import com.recruitment.model.Application;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Data Access Object for Candidate Job Applications.
 * Implements pure JDBC with PreparedStatements connecting to the normalized MySQL schema.
 */
public class ApplicationDAO {

    /**
     * Checks if a candidate has already applied for a specific job opening.
     */
    public boolean hasApplied(int candidateId, int jobId) {
        String sql = "SELECT 1 FROM applications WHERE candidate_id = ? AND job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
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
     * Submits a new job application with calculated match score and optional resume reference.
     */
    public boolean applyForJob(Application app) {
        String sql = "INSERT INTO applications (job_id, candidate_id, resume_id, cover_letter, match_score, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, app.getJobId());
            ps.setInt(2, app.getCandidateId());

            if (app.getResumeId() != null && app.getResumeId() > 0) {
                ps.setInt(3, app.getResumeId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, app.getCoverLetter());
            ps.setInt(5, app.getMatchScore());

            String status = app.getStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "Applied";
            }
            status = normalizeStatusForDb(status);
            ps.setString(6, status);

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
     * Retrieves an application by primary key with joined candidate, job, company, and resume details.
     */
    public Application getApplicationById(int applicationId) {
        String sql = "SELECT a.*, " +
                     "  c.full_name AS candidate_name, u.email AS candidate_email, c.phone AS candidate_phone, " +
                     "  c.city AS candidate_city, c.country AS candidate_country, c.profile_completion, " +
                     "  j.title AS job_title, j.location AS job_location, j.job_type, j.salary_range, " +
                     "  j.status AS job_status, j.recruiter_id, " +
                     "  comp.name AS company_name, " +
                     "  r.file_name AS resume_file_name, r.file_path AS resume_file_path " +
                     "FROM applications a " +
                     "JOIN candidates c ON a.candidate_id = c.candidate_id " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "JOIN jobs j ON a.job_id = j.job_id " +
                     "JOIN companies comp ON j.company_id = comp.company_id " +
                     "LEFT JOIN resumes r ON a.resume_id = r.resume_id " +
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
     * Retrieves all applications submitted by a candidate, ordered by application recency.
     */
    public List<Application> getApplicationsByCandidate(int candidateId) {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.*, " +
                     "  c.full_name AS candidate_name, u.email AS candidate_email, c.phone AS candidate_phone, " +
                     "  c.city AS candidate_city, c.country AS candidate_country, c.profile_completion, " +
                     "  j.title AS job_title, j.location AS job_location, j.job_type, j.salary_range, " +
                     "  j.status AS job_status, j.recruiter_id, " +
                     "  comp.name AS company_name, " +
                     "  r.file_name AS resume_file_name, r.file_path AS resume_file_path " +
                     "FROM applications a " +
                     "JOIN candidates c ON a.candidate_id = c.candidate_id " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "JOIN jobs j ON a.job_id = j.job_id " +
                     "JOIN companies comp ON j.company_id = comp.company_id " +
                     "LEFT JOIN resumes r ON a.resume_id = r.resume_id " +
                     "WHERE a.candidate_id = ? " +
                     "ORDER BY a.applied_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapApplication(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationsByCandidate] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Backward-compatible alias for legacy calls.
     */
    public List<Application> getApplicationsByApplicant(int applicantId) {
        return getApplicationsByCandidate(applicantId);
    }

    /**
     * Retrieves all applications received for vacancies posted by a specific recruiter,
     * with optional filtering by job ID and status.
     */
    public List<Application> getApplicationsByRecruiter(int recruiterId, Integer jobId, String statusFilter) {
        List<Application> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, " +
                "  c.full_name AS candidate_name, u.email AS candidate_email, c.phone AS candidate_phone, " +
                "  c.city AS candidate_city, c.country AS candidate_country, c.profile_completion, " +
                "  j.title AS job_title, j.location AS job_location, j.job_type, j.salary_range, " +
                "  j.status AS job_status, j.recruiter_id, " +
                "  comp.name AS company_name, " +
                "  r.file_name AS resume_file_name, r.file_path AS resume_file_path " +
                "FROM applications a " +
                "JOIN candidates c ON a.candidate_id = c.candidate_id " +
                "JOIN users u ON c.user_id = u.user_id " +
                "JOIN jobs j ON a.job_id = j.job_id " +
                "JOIN companies comp ON j.company_id = comp.company_id " +
                "LEFT JOIN resumes r ON a.resume_id = r.resume_id " +
                "WHERE j.recruiter_id = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        if (jobId != null && jobId > 0) {
            sql.append("AND a.job_id = ? ");
            params.add(jobId);
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
            sql.append("AND a.status = ? ");
            params.add(normalizeStatusForDb(statusFilter));
        }

        sql.append("ORDER BY a.applied_at DESC");

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
     * Allowed: Applied, Under_Review, Shortlisted, Interview_Scheduled, Selected, Rejected
     */
    public boolean updateApplicationStatus(int applicationId, String status) {
        String dbStatus = normalizeStatusForDb(status);
        String sql = "UPDATE applications SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE application_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dbStatus);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.updateApplicationStatus] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Allows a candidate to withdraw an application if it is still in early stage ('Applied' or 'Under_Review').
     */
    public boolean withdrawApplication(int applicationId, int candidateId) {
        String sql = "DELETE FROM applications WHERE application_id = ? AND candidate_id = ? AND status IN ('Applied', 'Under_Review')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, applicationId);
            ps.setInt(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.withdrawApplication] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves application counts aggregated by status for a recruiter's vacancies.
     */
    public Map<String, Integer> getApplicationStatsForRecruiter(int recruiterId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("applied", 0);
        stats.put("underReview", 0);
        stats.put("shortlisted", 0);
        stats.put("interviewScheduled", 0);
        stats.put("selected", 0);
        stats.put("rejected", 0);

        String sql = "SELECT a.status, COUNT(*) AS cnt " +
                     "FROM applications a " +
                     "JOIN jobs j ON a.job_id = j.job_id " +
                     "WHERE j.recruiter_id = ? " +
                     "GROUP BY a.status";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String st = rs.getString("status");
                    int count = rs.getInt("cnt");
                    total += count;

                    if ("Applied".equalsIgnoreCase(st)) {
                        stats.put("applied", count);
                    } else if ("Under_Review".equalsIgnoreCase(st) || "Under Review".equalsIgnoreCase(st)) {
                        stats.put("underReview", count);
                    } else if ("Shortlisted".equalsIgnoreCase(st)) {
                        stats.put("shortlisted", count);
                    } else if ("Interview_Scheduled".equalsIgnoreCase(st) || "Interview Scheduled".equalsIgnoreCase(st)) {
                        stats.put("interviewScheduled", count);
                    } else if ("Selected".equalsIgnoreCase(st)) {
                        stats.put("selected", count);
                    } else if ("Rejected".equalsIgnoreCase(st)) {
                        stats.put("rejected", count);
                    }
                }
                stats.put("total", total);
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationStatsForRecruiter] Error: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Retrieves application counts for a candidate.
     */
    public Map<String, Integer> getApplicationStatsForCandidate(int candidateId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("applied", 0);
        stats.put("underReview", 0);
        stats.put("shortlisted", 0);
        stats.put("interviewScheduled", 0);
        stats.put("selected", 0);
        stats.put("rejected", 0);

        String sql = "SELECT status, COUNT(*) AS cnt FROM applications WHERE candidate_id = ? GROUP BY status";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    String st = rs.getString("status");
                    int count = rs.getInt("cnt");
                    total += count;

                    if ("Applied".equalsIgnoreCase(st)) {
                        stats.put("applied", count);
                    } else if ("Under_Review".equalsIgnoreCase(st) || "Under Review".equalsIgnoreCase(st)) {
                        stats.put("underReview", count);
                    } else if ("Shortlisted".equalsIgnoreCase(st)) {
                        stats.put("shortlisted", count);
                    } else if ("Interview_Scheduled".equalsIgnoreCase(st) || "Interview Scheduled".equalsIgnoreCase(st)) {
                        stats.put("interviewScheduled", count);
                    } else if ("Selected".equalsIgnoreCase(st)) {
                        stats.put("selected", count);
                    } else if ("Rejected".equalsIgnoreCase(st)) {
                        stats.put("rejected", count);
                    }
                }
                stats.put("total", total);
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getApplicationStatsForCandidate] Error: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Resolves the latest uploaded resume ID for a candidate.
     */
    public Integer getCandidateLatestResumeId(int candidateId) {
        String sql = "SELECT resume_id FROM resumes WHERE candidate_id = ? ORDER BY uploaded_at DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("resume_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.getCandidateLatestResumeId] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Saves an uploaded resume file reference in the `resumes` table and returns its generated ID.
     */
    public int saveResume(int candidateId, String fileName, String filePath) {
        String sql = "INSERT INTO resumes (candidate_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, candidateId);
            ps.setString(2, fileName);
            ps.setString(3, filePath);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicationDAO.saveResume] Error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Maps a ResultSet row to a fully populated Application model.
     */
    private Application mapApplication(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setApplicationId(rs.getInt("application_id"));
        a.setJobId(rs.getInt("job_id"));
        a.setCandidateId(rs.getInt("candidate_id"));

        int resId = rs.getInt("resume_id");
        if (!rs.wasNull()) {
            a.setResumeId(resId);
        }

        a.setCoverLetter(rs.getString("cover_letter"));
        a.setMatchScore(rs.getInt("match_score"));

        String rawStatus = rs.getString("status");
        a.setStatus(formatStatusForDisplay(rawStatus));

        a.setAppliedAt(rs.getTimestamp("applied_at"));
        a.setUpdatedAt(rs.getTimestamp("updated_at"));

        // Candidate profile details
        a.setCandidateName(rs.getString("candidate_name"));
        a.setCandidateEmail(rs.getString("candidate_email"));
        a.setCandidatePhone(rs.getString("candidate_phone"));
        a.setCandidateCity(rs.getString("candidate_city"));
        a.setCandidateCountry(rs.getString("candidate_country"));
        a.setProfileCompletion(rs.getInt("profile_completion"));

        // Job details
        a.setJobTitle(rs.getString("job_title"));
        a.setJobLocation(rs.getString("job_location"));
        a.setJobType(rs.getString("job_type"));
        a.setSalaryRange(rs.getString("salary_range"));
        a.setJobStatus(rs.getString("job_status"));
        a.setRecruiterId(rs.getInt("recruiter_id"));
        a.setCompanyName(rs.getString("company_name"));

        // Resume details
        String resumeFile = rs.getString("resume_file_name");
        a.setResumeFileName(resumeFile);
        a.setResumePath(rs.getString("resume_file_path"));

        // Derive match level from stored score
        int score = a.getMatchScore();
        if (score >= 85) {
            a.setMatchLevel("EXCELLENT");
        } else if (score >= 70) {
            a.setMatchLevel("STRONG");
        } else if (score >= 50) {
            a.setMatchLevel("MODERATE");
        } else {
            a.setMatchLevel("LOW");
        }

        return a;
    }

    /**
     * Converts display statuses like "Under Review" to DB enum "Under_Review".
     */
    public static String normalizeStatusForDb(String status) {
        if (status == null) return "Applied";
        String s = status.trim().replace(" ", "_");
        if ("Under_Review".equalsIgnoreCase(s)) return "Under_Review";
        if ("Interview_Scheduled".equalsIgnoreCase(s)) return "Interview_Scheduled";
        if ("Shortlisted".equalsIgnoreCase(s)) return "Shortlisted";
        if ("Selected".equalsIgnoreCase(s)) return "Selected";
        if ("Rejected".equalsIgnoreCase(s)) return "Rejected";
        return "Applied";
    }

    /**
     * Converts DB enum "Under_Review" to user-friendly "Under Review".
     */
    public static String formatStatusForDisplay(String status) {
        if (status == null) return "Applied";
        return status.replace("_", " ");
    }
}
