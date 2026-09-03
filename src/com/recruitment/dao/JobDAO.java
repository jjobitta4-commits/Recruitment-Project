package com.recruitment.dao;

import com.recruitment.model.Job;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Jobs (Posting, Querying, Searching, Filtering, Updating, Deleting).
 */
public class JobDAO {

    /**
     * Posts a new job vacancy.
     */
    public boolean createJob(Job j) {
        String sql = "INSERT INTO jobs (recruiter_id, title, company, description, skills_required, " +
                "education_required, experience_required, salary_range, job_type, location, country, vacancies, deadline, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, j.getRecruiterId());
            ps.setString(2, j.getTitle());
            ps.setString(3, j.getCompany());
            ps.setString(4, j.getDescription());
            ps.setString(5, j.getSkillsRequired());
            ps.setString(6, j.getEducationRequired());
            ps.setString(7, j.getExperienceRequired());
            ps.setString(8, j.getSalaryRange());
            ps.setString(9, j.getJobType());
            ps.setString(10, j.getLocation());
            ps.setString(11, j.getCountry());
            ps.setInt(12, j.getVacancies() > 0 ? j.getVacancies() : 1);
            ps.setDate(13, j.getDeadline());
            ps.setString(14, (j.getStatus() != null && !j.getStatus().isEmpty()) ? j.getStatus() : "Active");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        j.setJobId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.createJob] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves a job by ID.
     */
    public Job getJobById(int jobId) {
        String sql = "SELECT j.*, COUNT(a.application_id) AS app_count FROM jobs j " +
                "LEFT JOIN applications a ON j.job_id = a.job_id WHERE j.job_id = ? GROUP BY j.job_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapJob(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getJobById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all active jobs with application counts.
     */
    public List<Job> getAllActiveJobs() {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT j.*, COUNT(a.application_id) AS app_count FROM jobs j " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.status = 'Active' " +
                "GROUP BY j.job_id ORDER BY j.posted_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapJob(rs));
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getAllActiveJobs] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all jobs posted by a specific recruiter.
     */
    public List<Job> getJobsByRecruiter(int recruiterId) {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT j.*, COUNT(a.application_id) AS app_count FROM jobs j " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.recruiter_id = ? " +
                "GROUP BY j.job_id ORDER BY j.posted_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJob(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getJobsByRecruiter] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing job.
     */
    public boolean updateJob(Job j) {
        String sql = "UPDATE jobs SET title = ?, company = ?, description = ?, skills_required = ?, " +
                "education_required = ?, experience_required = ?, salary_range = ?, job_type = ?, " +
                "location = ?, country = ?, vacancies = ?, deadline = ?, status = ? WHERE job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, j.getTitle());
            ps.setString(2, j.getCompany());
            ps.setString(3, j.getDescription());
            ps.setString(4, j.getSkillsRequired());
            ps.setString(5, j.getEducationRequired());
            ps.setString(6, j.getExperienceRequired());
            ps.setString(7, j.getSalaryRange());
            ps.setString(8, j.getJobType());
            ps.setString(9, j.getLocation());
            ps.setString(10, j.getCountry());
            ps.setInt(11, j.getVacancies());
            ps.setDate(12, j.getDeadline());
            ps.setString(13, j.getStatus());
            ps.setInt(14, j.getJobId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JobDAO.updateJob] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates status (e.g. 'Active', 'Closed').
     */
    public boolean updateJobStatus(int jobId, String status) {
        String sql = "UPDATE jobs SET status = ? WHERE job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JobDAO.updateJobStatus] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Deletes a job posting.
     */
    public boolean deleteJob(int jobId) {
        String sql = "DELETE FROM jobs WHERE job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JobDAO.deleteJob] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Searches and filters jobs based on multiple criteria.
     */
    public List<Job> searchJobs(String keyword, String jobType, String country, String location, String skill, String experience) {
        List<Job> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT j.*, COUNT(a.application_id) AS app_count FROM jobs j " +
                "LEFT JOIN applications a ON j.job_id = a.job_id WHERE j.status = 'Active' ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(j.title) LIKE ? OR LOWER(j.company) LIKE ? OR LOWER(j.description) LIKE ? OR LOWER(j.skills_required) LIKE ?) ");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (jobType != null && !jobType.trim().isEmpty() && !jobType.equalsIgnoreCase("All")) {
            sql.append("AND LOWER(j.job_type) = ? ");
            params.add(jobType.trim().toLowerCase());
        }
        if (country != null && !country.trim().isEmpty()) {
            sql.append("AND LOWER(j.country) LIKE ? ");
            params.add("%" + country.trim().toLowerCase() + "%");
        }
        if (location != null && !location.trim().isEmpty()) {
            sql.append("AND LOWER(j.location) LIKE ? ");
            params.add("%" + location.trim().toLowerCase() + "%");
        }
        if (skill != null && !skill.trim().isEmpty()) {
            sql.append("AND LOWER(j.skills_required) LIKE ? ");
            params.add("%" + skill.trim().toLowerCase() + "%");
        }
        if (experience != null && !experience.trim().isEmpty() && !experience.equalsIgnoreCase("All")) {
            sql.append("AND LOWER(j.experience_required) LIKE ? ");
            params.add("%" + experience.trim().toLowerCase() + "%");
        }

        sql.append("GROUP BY j.job_id ORDER BY j.posted_date DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJob(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.searchJobs] Error: " + e.getMessage());
        }

        return list;
    }

    private Job mapJob(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setJobId(rs.getInt("job_id"));
        j.setRecruiterId(rs.getInt("recruiter_id"));
        j.setTitle(rs.getString("title"));
        j.setCompany(rs.getString("company"));
        j.setDescription(rs.getString("description"));
        j.setSkillsRequired(rs.getString("skills_required"));
        j.setEducationRequired(rs.getString("education_required"));
        j.setExperienceRequired(rs.getString("experience_required"));
        j.setSalaryRange(rs.getString("salary_range"));
        j.setJobType(rs.getString("job_type"));
        j.setLocation(rs.getString("location"));
        j.setCountry(rs.getString("country"));
        j.setVacancies(rs.getInt("vacancies"));
        j.setDeadline(rs.getDate("deadline"));
        j.setStatus(rs.getString("status"));
        j.setPostedDate(rs.getTimestamp("posted_date"));

        try {
            j.setApplicationCount(rs.getInt("app_count"));
        } catch (SQLException ignored) {}

        return j;
    }
}
