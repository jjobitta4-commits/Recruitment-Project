package com.recruitment.dao;

import com.recruitment.model.Job;
import com.recruitment.model.JobSkill;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Data Access Object for Jobs and Job Skills.
 * Implements full relational mapping across 'jobs', 'companies', 'recruiters', 'job_skills', and 'skills'.
 */
public class JobDAO {

    /**
     * Creates a new job posting with its mandatory and preferred skills in a single database transaction.
     */
    public boolean createJob(Job j, List<JobSkill> skills) {
        String insertJobSql = "INSERT INTO jobs (company_id, recruiter_id, title, description, job_type, " +
                "location, salary_range, min_experience_years, min_education, vacancies, deadline, approval_status, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertSkillSql = "INSERT INTO job_skills (job_id, skill_id, is_mandatory, min_years_required) " +
                "VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psJob = conn.prepareStatement(insertJobSql, Statement.RETURN_GENERATED_KEYS)) {
                psJob.setInt(1, j.getCompanyId());
                psJob.setInt(2, j.getRecruiterId());
                psJob.setString(3, j.getTitle());
                psJob.setString(4, j.getDescription());
                psJob.setString(5, j.getJobType() != null ? j.getJobType() : "Full Time");
                psJob.setString(6, j.getLocation());
                psJob.setString(7, j.getSalaryRange());
                psJob.setInt(8, j.getMinExperienceYears());
                psJob.setString(9, j.getMinEducation());
                psJob.setInt(10, j.getVacancies() > 0 ? j.getVacancies() : 1);
                psJob.setDate(11, j.getDeadline());
                psJob.setString(12, j.getApprovalStatus() != null ? j.getApprovalStatus() : "approved");
                psJob.setString(13, j.getStatus() != null ? j.getStatus() : "Active");

                int affected = psJob.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                int jobId = -1;
                try (ResultSet rs = psJob.getGeneratedKeys()) {
                    if (rs.next()) {
                        jobId = rs.getInt(1);
                        j.setJobId(jobId);
                    }
                }

                if (jobId <= 0) {
                    conn.rollback();
                    return false;
                }

                // Batch insert Job Skills
                if (skills != null && !skills.isEmpty()) {
                    try (PreparedStatement psSkill = conn.prepareStatement(insertSkillSql)) {
                        for (JobSkill js : skills) {
                            if (js.getSkillId() > 0) {
                                psSkill.setInt(1, jobId);
                                psSkill.setInt(2, js.getSkillId());
                                psSkill.setBoolean(3, js.isMandatory());
                                psSkill.setInt(4, js.getMinYearsRequired() > 0 ? js.getMinYearsRequired() : 1);
                                psSkill.addBatch();
                            }
                        }
                        psSkill.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (conn != null) conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.createJob] Transaction Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    /**
     * Retrieves a job by ID along with its associated skills, company, and recruiter.
     */
    public Job getJobById(int jobId) {
        String sql = "SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.job_id = ? " +
                "GROUP BY j.job_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Job j = mapJob(rs);
                    j.setJobSkills(getSkillsForJob(conn, jobId));
                    return j;
                }
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getJobById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all active and approved jobs visible to candidates.
     */
    public List<Job> getAllActiveJobs() {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.status = 'Active' AND j.approval_status = 'approved' " +
                "GROUP BY j.job_id ORDER BY j.posted_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Job j = mapJob(rs);
                list.add(j);
            }
            // Populate skills
            for (Job j : list) {
                j.setJobSkills(getSkillsForJob(conn, j.getJobId()));
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
        String sql = "SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.recruiter_id = ? " +
                "GROUP BY j.job_id ORDER BY j.posted_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJob(rs));
                }
            }
            for (Job j : list) {
                j.setJobSkills(getSkillsForJob(conn, j.getJobId()));
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getJobsByRecruiter] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all jobs awaiting Admin approval.
     */
    public List<Job> getPendingJobs() {
        List<Job> list = new ArrayList<>();
        String sql = "SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.approval_status = 'pending' " +
                "GROUP BY j.job_id ORDER BY j.posted_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapJob(rs));
            }
            for (Job j : list) {
                j.setJobSkills(getSkillsForJob(conn, j.getJobId()));
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getPendingJobs] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all jobs across platform for Admin overview.
     */
    public List<Job> getAllJobsForAdmin(String filterApproval) {
        List<Job> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id ");

        if (filterApproval != null && !filterApproval.equalsIgnoreCase("all") && !filterApproval.trim().isEmpty()) {
            sql.append("WHERE j.approval_status = ? ");
        }
        sql.append("GROUP BY j.job_id ORDER BY j.posted_at DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (filterApproval != null && !filterApproval.equalsIgnoreCase("all") && !filterApproval.trim().isEmpty()) {
                ps.setString(1, filterApproval);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJob(rs));
                }
            }
            for (Job j : list) {
                j.setJobSkills(getSkillsForJob(conn, j.getJobId()));
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getAllJobsForAdmin] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates moderation approval status (pending, approved, rejected).
     */
    public boolean updateJobApprovalStatus(int jobId, String approvalStatus) {
        String sql = "UPDATE jobs SET approval_status = ? WHERE job_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, approvalStatus);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JobDAO.updateJobApprovalStatus] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates listing status (Active, Closed).
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
     * Updates job details and refreshes job skills in a transaction.
     */
    public boolean updateJob(Job j, List<JobSkill> skills) {
        String updateJobSql = "UPDATE jobs SET title = ?, description = ?, job_type = ?, location = ?, " +
                "salary_range = ?, min_experience_years = ?, min_education = ?, vacancies = ?, deadline = ?, status = ? " +
                "WHERE job_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psJob = conn.prepareStatement(updateJobSql)) {
                psJob.setString(1, j.getTitle());
                psJob.setString(2, j.getDescription());
                psJob.setString(3, j.getJobType());
                psJob.setString(4, j.getLocation());
                psJob.setString(5, j.getSalaryRange());
                psJob.setInt(6, j.getMinExperienceYears());
                psJob.setString(7, j.getMinEducation());
                psJob.setInt(8, j.getVacancies());
                psJob.setDate(9, j.getDeadline());
                psJob.setString(10, j.getStatus());
                psJob.setInt(11, j.getJobId());

                int affected = psJob.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                // If new skills list provided, update job_skills
                if (skills != null) {
                    try (PreparedStatement delPs = conn.prepareStatement("DELETE FROM job_skills WHERE job_id = ?")) {
                        delPs.setInt(1, j.getJobId());
                        delPs.executeUpdate();
                    }

                    try (PreparedStatement insPs = conn.prepareStatement(
                            "INSERT INTO job_skills (job_id, skill_id, is_mandatory, min_years_required) VALUES (?, ?, ?, ?)")) {
                        for (JobSkill js : skills) {
                            if (js.getSkillId() > 0) {
                                insPs.setInt(1, j.getJobId());
                                insPs.setInt(2, js.getSkillId());
                                insPs.setBoolean(3, js.isMandatory());
                                insPs.setInt(4, js.getMinYearsRequired());
                                insPs.addBatch();
                            }
                        }
                        insPs.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (conn != null) conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.updateJob] Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
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
     * Searches approved active jobs using multi-criteria filtering.
     */
    public List<Job> searchJobs(String keyword, String jobType, String country, String location, String skill, String experience) {
        List<Job> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT j.*, c.name AS comp_name, r.name AS recruiter_name, " +
                "COUNT(a.application_id) AS app_count " +
                "FROM jobs j " +
                "LEFT JOIN companies c ON j.company_id = c.company_id " +
                "LEFT JOIN recruiters r ON j.recruiter_id = r.recruiter_id " +
                "LEFT JOIN applications a ON j.job_id = a.job_id " +
                "WHERE j.status = 'Active' AND j.approval_status = 'approved' ");

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(j.title) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(j.description) LIKE ?) ");
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (jobType != null && !jobType.trim().isEmpty() && !jobType.equalsIgnoreCase("All")) {
            sql.append("AND LOWER(j.job_type) = ? ");
            params.add(jobType.trim().toLowerCase());
        }
        if (location != null && !location.trim().isEmpty()) {
            sql.append("AND LOWER(j.location) LIKE ? ");
            params.add("%" + location.trim().toLowerCase() + "%");
        }
        if (experience != null && !experience.trim().isEmpty() && !experience.equalsIgnoreCase("All")) {
            try {
                int expYears = Integer.parseInt(experience.trim());
                sql.append("AND j.min_experience_years <= ? ");
                params.add(expYears);
            } catch (NumberFormatException ignored) {}
        }
        if (skill != null && !skill.trim().isEmpty()) {
            sql.append("AND j.job_id IN (SELECT js.job_id FROM job_skills js JOIN skills s ON js.skill_id = s.skill_id WHERE LOWER(s.name) LIKE ?) ");
            params.add("%" + skill.trim().toLowerCase() + "%");
        }

        sql.append("GROUP BY j.job_id ORDER BY j.posted_at DESC");

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
            for (Job j : list) {
                j.setJobSkills(getSkillsForJob(conn, j.getJobId()));
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.searchJobs] Error: " + e.getMessage());
        }

        return list;
    }

    /**
     * Helper to retrieve all JobSkill records associated with a job.
     */
    public List<JobSkill> getSkillsForJob(Connection conn, int jobId) {
        List<JobSkill> skills = new ArrayList<>();
        String sql = "SELECT js.*, s.name AS skill_name, s.category AS skill_cat " +
                "FROM job_skills js " +
                "JOIN skills s ON js.skill_id = s.skill_id " +
                "WHERE js.job_id = ? " +
                "ORDER BY js.is_mandatory DESC, js.min_years_required DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JobSkill js = new JobSkill();
                    js.setId(rs.getInt("id"));
                    js.setJobId(rs.getInt("job_id"));
                    js.setSkillId(rs.getInt("skill_id"));
                    js.setSkillName(rs.getString("skill_name"));
                    js.setCategory(rs.getString("skill_cat"));
                    js.setMandatory(rs.getBoolean("is_mandatory"));
                    js.setMinYearsRequired(rs.getInt("min_years_required"));
                    skills.add(js);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JobDAO.getSkillsForJob] Error: " + e.getMessage());
        }
        return skills;
    }

    private Job mapJob(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.setJobId(rs.getInt("job_id"));
        j.setCompanyId(rs.getInt("company_id"));
        j.setRecruiterId(rs.getInt("recruiter_id"));
        j.setTitle(rs.getString("title"));
        j.setDescription(rs.getString("description"));
        j.setJobType(rs.getString("job_type"));
        j.setLocation(rs.getString("location"));
        j.setSalaryRange(rs.getString("salary_range"));
        j.setMinExperienceYears(rs.getInt("min_experience_years"));
        j.setMinEducation(rs.getString("min_education"));
        j.setVacancies(rs.getInt("vacancies"));
        j.setDeadline(rs.getDate("deadline"));
        j.setApprovalStatus(rs.getString("approval_status"));
        j.setStatus(rs.getString("status"));
        j.setPostedDate(rs.getTimestamp("posted_at"));

        try { j.setCompany(rs.getString("comp_name")); } catch (SQLException ignored) {}
        try { j.setRecruiterName(rs.getString("recruiter_name")); } catch (SQLException ignored) {}
        try { j.setApplicationCount(rs.getInt("app_count")); } catch (SQLException ignored) {}

        return j;
    }
}
