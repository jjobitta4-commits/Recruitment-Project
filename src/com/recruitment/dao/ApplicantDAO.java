package com.recruitment.dao;

import com.recruitment.model.Applicant;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Applicant profiles.
 */
public class ApplicantDAO {

    /**
     * Creates a new Applicant record for a registered user.
     */
    public boolean createApplicant(Applicant a) {
        String sql = "INSERT INTO applicants (user_id, full_name, phone, dob, gender, address, city, country, " +
                "education, university, graduation_year, skills, experience_years, expected_salary, linkedin_url, " +
                "github_url, leetcode_url, resume_file) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getUserId());
            ps.setString(2, a.getFullName());
            ps.setString(3, a.getPhone());
            ps.setDate(4, a.getDob());
            ps.setString(5, a.getGender());
            ps.setString(6, a.getAddress());
            ps.setString(7, a.getCity());
            ps.setString(8, a.getCountry());
            ps.setString(9, a.getEducation());
            ps.setString(10, a.getUniversity());
            if (a.getGraduationYear() != null) ps.setInt(11, a.getGraduationYear()); else ps.setNull(11, Types.INTEGER);
            ps.setString(12, a.getSkills());
            ps.setInt(13, a.getExperienceYears());
            ps.setString(14, a.getExpectedSalary());
            ps.setString(15, a.getLinkedinUrl());
            ps.setString(16, a.getGithubUrl());
            ps.setString(17, a.getLeetcodeUrl());
            ps.setString(18, a.getResumeFile());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        a.setApplicantId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.createApplicant] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets applicant profile by userId.
     */
    public Applicant getApplicantByUserId(int userId) {
        String sql = "SELECT a.*, u.email FROM applicants a JOIN users u ON a.user_id = u.user_id WHERE a.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapApplicant(rs);
                }
            }
        } catch (SQLException ignored) {}

        String candSql = "SELECT c.candidate_id AS applicant_id, c.user_id, c.full_name, c.phone, c.dob, c.gender, " +
                         "c.address, c.city, c.country, c.bio, u.email " +
                         "FROM candidates c JOIN users u ON c.user_id = u.user_id WHERE c.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(candSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Applicant ap = new Applicant();
                    ap.setApplicantId(rs.getInt("applicant_id"));
                    ap.setUserId(rs.getInt("user_id"));
                    ap.setEmail(rs.getString("email"));
                    ap.setFullName(rs.getString("full_name"));
                    ap.setPhone(rs.getString("phone"));
                    ap.setDob(rs.getDate("dob"));
                    ap.setGender(rs.getString("gender"));
                    ap.setAddress(rs.getString("address"));
                    ap.setCity(rs.getString("city"));
                    ap.setCountry(rs.getString("country"));
                    return ap;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.getApplicantByUserId fallback] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets applicant profile by applicantId.
     */
    public Applicant getApplicantById(int applicantId) {
        String sql = "SELECT a.*, u.email FROM applicants a JOIN users u ON a.user_id = u.user_id WHERE a.applicant_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapApplicant(rs);
                }
            }
        } catch (SQLException ignored) {}

        String candSql = "SELECT c.candidate_id AS applicant_id, c.user_id, c.full_name, c.phone, c.dob, c.gender, " +
                         "c.address, c.city, c.country, c.bio, u.email " +
                         "FROM candidates c JOIN users u ON c.user_id = u.user_id WHERE c.candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(candSql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Applicant ap = new Applicant();
                    ap.setApplicantId(rs.getInt("applicant_id"));
                    ap.setUserId(rs.getInt("user_id"));
                    ap.setEmail(rs.getString("email"));
                    ap.setFullName(rs.getString("full_name"));
                    ap.setPhone(rs.getString("phone"));
                    ap.setDob(rs.getDate("dob"));
                    ap.setGender(rs.getString("gender"));
                    ap.setAddress(rs.getString("address"));
                    ap.setCity(rs.getString("city"));
                    ap.setCountry(rs.getString("country"));
                    return ap;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.getApplicantById fallback] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates full applicant profile.
     */
    public boolean updateApplicantProfile(Applicant a) {
        String sql = "UPDATE applicants SET full_name = ?, phone = ?, dob = ?, gender = ?, address = ?, " +
                "city = ?, country = ?, education = ?, university = ?, graduation_year = ?, skills = ?, " +
                "experience_years = ?, expected_salary = ?, linkedin_url = ?, github_url = ?, leetcode_url = ? " +
                "WHERE applicant_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getFullName());
            ps.setString(2, a.getPhone());
            ps.setDate(3, a.getDob());
            ps.setString(4, a.getGender());
            ps.setString(5, a.getAddress());
            ps.setString(6, a.getCity());
            ps.setString(7, a.getCountry());
            ps.setString(8, a.getEducation());
            ps.setString(9, a.getUniversity());
            if (a.getGraduationYear() != null) ps.setInt(10, a.getGraduationYear()); else ps.setNull(10, Types.INTEGER);
            ps.setString(11, a.getSkills());
            ps.setInt(12, a.getExperienceYears());
            ps.setString(13, a.getExpectedSalary());
            ps.setString(14, a.getLinkedinUrl());
            ps.setString(15, a.getGithubUrl());
            ps.setString(16, a.getLeetcodeUrl());
            ps.setInt(17, a.getApplicantId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.updateApplicantProfile] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates candidate resume filename.
     */
    public boolean updateResumeFile(int applicantId, String fileName) {
        String sql = "UPDATE applicants SET resume_file = ? WHERE applicant_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.setInt(2, applicantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.updateResumeFile] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns all applicants for search/directory.
     */
    public List<Applicant> getAllApplicants() {
        List<Applicant> list = new ArrayList<>();
        String sql = "SELECT a.*, u.email FROM applicants a JOIN users u ON a.user_id = u.user_id ORDER BY a.full_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapApplicant(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ApplicantDAO.getAllApplicants] Error: " + e.getMessage());
        }
        return list;
    }

    private Applicant mapApplicant(ResultSet rs) throws SQLException {
        Applicant a = new Applicant();
        a.setApplicantId(rs.getInt("applicant_id"));
        a.setUserId(rs.getInt("user_id"));
        a.setFullName(rs.getString("full_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setDob(rs.getDate("dob"));
        a.setGender(rs.getString("gender"));
        a.setAddress(rs.getString("address"));
        a.setCity(rs.getString("city"));
        a.setCountry(rs.getString("country"));
        a.setEducation(rs.getString("education"));
        a.setUniversity(rs.getString("university"));
        int gradYear = rs.getInt("graduation_year");
        a.setGraduationYear(rs.wasNull() ? null : gradYear);
        a.setSkills(rs.getString("skills"));
        a.setExperienceYears(rs.getInt("experience_years"));
        a.setExpectedSalary(rs.getString("expected_salary"));
        a.setLinkedinUrl(rs.getString("linkedin_url"));
        a.setGithubUrl(rs.getString("github_url"));
        a.setLeetcodeUrl(rs.getString("leetcode_url"));
        a.setResumeFile(rs.getString("resume_file"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
