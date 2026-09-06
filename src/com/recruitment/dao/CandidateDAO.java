package com.recruitment.dao;

import com.recruitment.model.*;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Candidate profiles, normalized skills, education, experience, and projects.
 * Exclusively uses PreparedStatement.
 */
public class CandidateDAO {

    public Candidate getCandidateByUserId(int userId) {
        String sql = "SELECT c.*, u.email FROM candidates c JOIN users u ON c.user_id = u.user_id WHERE c.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Candidate c = mapCandidate(rs);
                    c.setSkills(getSkillsByCandidateId(c.getCandidateId()));
                    c.setEducation(getEducationByCandidateId(c.getCandidateId()));
                    c.setExperience(getExperienceByCandidateId(c.getCandidateId()));
                    c.setProjects(getProjectsByCandidateId(c.getCandidateId()));
                    return c;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getCandidateByUserId] Error: " + e.getMessage());
        }
        return autoProvisionCandidateIfUserExists(userId);
    }

    public boolean createCandidate(Candidate c) {
        String sql = "INSERT INTO candidates (user_id, full_name, phone, dob, gender, address, city, country, bio, profile_completion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getUserId());
            ps.setString(2, c.getFullName() != null && !c.getFullName().trim().isEmpty() ? c.getFullName().trim() : "Candidate");
            ps.setString(3, c.getPhone() != null ? c.getPhone() : "");
            ps.setDate(4, c.getDob());
            ps.setString(5, c.getGender() != null ? c.getGender() : "");
            ps.setString(6, c.getAddress() != null ? c.getAddress() : "");
            ps.setString(7, c.getCity() != null ? c.getCity() : "");
            ps.setString(8, c.getCountry() != null ? c.getCountry() : "");
            ps.setString(9, c.getBio() != null ? c.getBio() : "");
            ps.setInt(10, c.getProfileCompletion() > 0 ? c.getProfileCompletion() : 20);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setCandidateId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.createCandidate] Error: " + e.getMessage());
        }
        return false;
    }

    private Candidate autoProvisionCandidateIfUserExists(int userId) {
        String userSql = "SELECT user_id, email, role FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(userSql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String email = rs.getString("email");
                    if ("applicant".equalsIgnoreCase(role) || "candidate".equalsIgnoreCase(role)) {
                        Candidate c = new Candidate();
                        c.setUserId(userId);
                        c.setEmail(email);
                        c.setFullName("Candidate");
                        c.setProfileCompletion(20);

                        // Check if applicant table has info to reuse
                        String appSql = "SELECT * FROM applicants WHERE user_id = ?";
                        try (PreparedStatement aps = conn.prepareStatement(appSql)) {
                            aps.setInt(1, userId);
                            try (ResultSet ars = aps.executeQuery()) {
                                if (ars.next()) {
                                    String name = ars.getString("full_name");
                                    if (name != null && !name.trim().isEmpty()) c.setFullName(name.trim());
                                    c.setPhone(ars.getString("phone"));
                                    c.setDob(ars.getDate("dob"));
                                    c.setGender(ars.getString("gender"));
                                    c.setAddress(ars.getString("address"));
                                    c.setCity(ars.getString("city"));
                                    c.setCountry(ars.getString("country"));
                                } else {
                                    int at = email.indexOf('@');
                                    if (at > 0) {
                                        c.setFullName(email.substring(0, at));
                                    }
                                }
                            }
                        }

                        boolean created = createCandidate(c);
                        if (created && c.getCandidateId() > 0) {
                            System.out.println("[CandidateDAO] Auto-provisioned candidates record #" + c.getCandidateId() + " for user #" + userId);
                            return c;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.autoProvisionCandidateIfUserExists] Error: " + e.getMessage());
        }
        return null;
    }

    public Candidate getCandidateById(int candidateId) {
        String sql = "SELECT c.*, u.email FROM candidates c JOIN users u ON c.user_id = u.user_id WHERE c.candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Candidate c = mapCandidate(rs);
                    c.setSkills(getSkillsByCandidateId(c.getCandidateId()));
                    c.setEducation(getEducationByCandidateId(c.getCandidateId()));
                    c.setExperience(getExperienceByCandidateId(c.getCandidateId()));
                    c.setProjects(getProjectsByCandidateId(c.getCandidateId()));
                    return c;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getCandidateById] Error: " + e.getMessage());
        }
        return null;
    }

    public boolean updateCandidateProfile(Candidate c) {
        String sql = "UPDATE candidates SET full_name = ?, phone = ?, dob = ?, gender = ?, address = ?, " +
                     "city = ?, country = ?, bio = ?, profile_completion = ? WHERE candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getFullName());
            ps.setString(2, c.getPhone());
            ps.setDate(3, c.getDob());
            ps.setString(4, c.getGender());
            ps.setString(5, c.getAddress());
            ps.setString(6, c.getCity());
            ps.setString(7, c.getCountry());
            ps.setString(8, c.getBio());
            ps.setInt(9, c.getProfileCompletion());
            ps.setInt(10, c.getCandidateId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.updateCandidateProfile] Error: " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // Master Skills Taxonomy & Candidate Skills
    // ==========================================
    public List<Skill> getAllMasterSkills() {
        List<Skill> list = new ArrayList<>();
        String sql = "SELECT skill_id, name, category FROM skills ORDER BY category, name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Skill(rs.getInt("skill_id"), rs.getString("name"), rs.getString("category")));
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getAllMasterSkills] Error: " + e.getMessage());
        }
        return list;
    }

    public Skill getOrCreateSkill(String name, String category) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        String selectSql = "SELECT skill_id, name, category FROM skills WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Skill(rs.getInt("skill_id"), rs.getString("name"), rs.getString("category"));
                    }
                }
            }
            String insertSql = "INSERT INTO skills (name, category) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, category != null && !category.isEmpty() ? category : "Technical");
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return new Skill(rs.getInt(1), name, category != null && !category.isEmpty() ? category : "Technical");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getOrCreateSkill] Error: " + e.getMessage());
        }
        return null;
    }

    public List<CandidateSkill> getSkillsByCandidateId(int candidateId) {
        List<CandidateSkill> list = new ArrayList<>();
        String sql = "SELECT cs.id, cs.candidate_id, cs.skill_id, s.name AS skill_name, s.category, " +
                     "cs.proficiency_level, cs.years_experience " +
                     "FROM candidate_skills cs JOIN skills s ON cs.skill_id = s.skill_id " +
                     "WHERE cs.candidate_id = ? ORDER BY s.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CandidateSkill(
                            rs.getInt("id"),
                            rs.getInt("candidate_id"),
                            rs.getInt("skill_id"),
                            rs.getString("skill_name"),
                            rs.getString("category"),
                            rs.getString("proficiency_level"),
                            rs.getInt("years_experience")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getSkillsByCandidateId] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addCandidateSkill(int candidateId, int skillId, String proficiency, int years) {
        String sql = "INSERT INTO candidate_skills (candidate_id, skill_id, proficiency_level, years_experience) " +
                     "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE proficiency_level = VALUES(proficiency_level), years_experience = VALUES(years_experience)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            ps.setInt(2, skillId);
            ps.setString(3, proficiency != null ? proficiency : "Intermediate");
            ps.setInt(4, Math.max(0, years));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.addCandidateSkill] Error: " + e.getMessage());
        }
        return false;
    }

    public boolean removeCandidateSkill(int candidateId, int skillId) {
        String sql = "DELETE FROM candidate_skills WHERE candidate_id = ? AND skill_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            ps.setInt(2, skillId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.removeCandidateSkill] Error: " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // Education Entries
    // ==========================================
    public List<Education> getEducationByCandidateId(int candidateId) {
        List<Education> list = new ArrayList<>();
        String sql = "SELECT education_id, candidate_id, degree, institution, field_of_study, start_year, end_year, grade_or_gpa " +
                     "FROM education WHERE candidate_id = ? ORDER BY end_year DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Education(
                            rs.getInt("education_id"),
                            rs.getInt("candidate_id"),
                            rs.getString("degree"),
                            rs.getString("institution"),
                            rs.getString("field_of_study"),
                            rs.getObject("start_year") != null ? rs.getInt("start_year") : null,
                            rs.getObject("end_year") != null ? rs.getInt("end_year") : null,
                            rs.getString("grade_or_gpa")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getEducationByCandidateId] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addEducation(Education edu) {
        String sql = "INSERT INTO education (candidate_id, degree, institution, field_of_study, start_year, end_year, grade_or_gpa) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, edu.getCandidateId());
            ps.setString(2, edu.getDegree());
            ps.setString(3, edu.getInstitution());
            ps.setString(4, edu.getFieldOfStudy());
            if (edu.getStartYear() != null) ps.setInt(5, edu.getStartYear()); else ps.setNull(5, Types.INTEGER);
            if (edu.getEndYear() != null) ps.setInt(6, edu.getEndYear()); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, edu.getGradeOrGpa());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.addEducation] Error: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteEducation(int educationId, int candidateId) {
        String sql = "DELETE FROM education WHERE education_id = ? AND candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, educationId);
            ps.setInt(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.deleteEducation] Error: " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // Experience Entries
    // ==========================================
    public List<Experience> getExperienceByCandidateId(int candidateId) {
        List<Experience> list = new ArrayList<>();
        String sql = "SELECT experience_id, candidate_id, company_name, job_title, start_date, end_date, is_current, description " +
                     "FROM experience WHERE candidate_id = ? ORDER BY start_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Experience(
                            rs.getInt("experience_id"),
                            rs.getInt("candidate_id"),
                            rs.getString("company_name"),
                            rs.getString("job_title"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getBoolean("is_current"),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getExperienceByCandidateId] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addExperience(Experience exp) {
        String sql = "INSERT INTO experience (candidate_id, company_name, job_title, start_date, end_date, is_current, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, exp.getCandidateId());
            ps.setString(2, exp.getCompanyName());
            ps.setString(3, exp.getJobTitle());
            ps.setDate(4, exp.getStartDate());
            ps.setDate(5, exp.getEndDate());
            ps.setBoolean(6, exp.isCurrent());
            ps.setString(7, exp.getDescription());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.addExperience] Error: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteExperience(int experienceId, int candidateId) {
        String sql = "DELETE FROM experience WHERE experience_id = ? AND candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, experienceId);
            ps.setInt(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.deleteExperience] Error: " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // Project Entries
    // ==========================================
    public List<Project> getProjectsByCandidateId(int candidateId) {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT project_id, candidate_id, title, technologies_used, project_url, description " +
                     "FROM projects WHERE candidate_id = ? ORDER BY project_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Project(
                            rs.getInt("project_id"),
                            rs.getInt("candidate_id"),
                            rs.getString("title"),
                            rs.getString("technologies_used"),
                            rs.getString("project_url"),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.getProjectsByCandidateId] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean addProject(Project proj) {
        String sql = "INSERT INTO projects (candidate_id, title, technologies_used, project_url, description) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, proj.getCandidateId());
            ps.setString(2, proj.getTitle());
            ps.setString(3, proj.getTechnologiesUsed());
            ps.setString(4, proj.getProjectUrl());
            ps.setString(5, proj.getDescription());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.addProject] Error: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteProject(int projectId, int candidateId) {
        String sql = "DELETE FROM projects WHERE project_id = ? AND candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.deleteProject] Error: " + e.getMessage());
        }
        return false;
    }

    // ==========================================
    // Resume Storage
    // ==========================================
    public boolean saveResume(int candidateId, String filePath, String parsedText, String parsedSkills) {
        String sql = "INSERT INTO resumes (candidate_id, file_path, parsed_text, parsed_skills) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            ps.setString(2, filePath);
            ps.setString(3, parsedText);
            ps.setString(4, parsedSkills);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CandidateDAO.saveResume] Error: " + e.getMessage());
        }
        return false;
    }

    public void updateProfileCompletion(int candidateId, int completion) {
        String sql = "UPDATE candidates SET profile_completion = ? WHERE candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, completion);
            ps.setInt(2, candidateId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private Candidate mapCandidate(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setCandidateId(rs.getInt("candidate_id"));
        c.setUserId(rs.getInt("user_id"));
        try { c.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setDob(rs.getDate("dob"));
        c.setGender(rs.getString("gender"));
        c.setAddress(rs.getString("address"));
        c.setCity(rs.getString("city"));
        c.setCountry(rs.getString("country"));
        c.setBio(rs.getString("bio"));
        c.setProfileCompletion(rs.getInt("profile_completion"));
        return c;
    }
}
