package com.recruitment.dao;

import com.recruitment.model.CareerMilestone;
import com.recruitment.model.CareerPath;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Career Paths and Milestones.
 * Aligned with 'career_paths' and 'career_milestones' tables.
 * Exclusively utilizes PreparedStatement.
 */
public class CareerPathDAO {

    public List<CareerPath> getAllCareerPaths() {
        List<CareerPath> list = new ArrayList<>();
        String sql = "SELECT * FROM career_paths ORDER BY path_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                CareerPath cp = mapCareerPath(rs);
                cp.setMilestones(getMilestonesByPathId(conn, cp.getPathId()));
                list.add(cp);
            }
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.getAllCareerPaths] Error: " + e.getMessage());
        }
        return list;
    }

    public CareerPath getCareerPathById(int pathId) {
        String sql = "SELECT * FROM career_paths WHERE path_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pathId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CareerPath cp = mapCareerPath(rs);
                    cp.setMilestones(getMilestonesByPathId(conn, cp.getPathId()));
                    return cp;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.getCareerPathById] Error: " + e.getMessage());
        }
        return null;
    }

    public List<CareerMilestone> getMilestonesByPathId(int pathId) {
        try (Connection conn = DBConnection.getConnection()) {
            return getMilestonesByPathId(conn, pathId);
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.getMilestonesByPathId] Error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CareerMilestone> getMilestonesByPathId(Connection conn, int pathId) {
        List<CareerMilestone> list = new ArrayList<>();
        String sql = "SELECT * FROM career_milestones WHERE path_id = ? ORDER BY level_order ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pathId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCareerMilestone(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.getMilestonesByPathId(conn)] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean createCareerPath(CareerPath path) {
        String sql = "INSERT INTO career_paths (title, category, description, required_core_skills, " +
                     "min_starting_experience_years, average_market_salary) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, path.getTitle());
            ps.setString(2, path.getCategory());
            ps.setString(3, path.getDescription());
            ps.setString(4, path.getRequiredCoreSkills());
            ps.setInt(5, path.getMinStartingExperienceYears());
            ps.setString(6, path.getAverageMarketSalary());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        path.setPathId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.createCareerPath] Error: " + e.getMessage());
        }
        return false;
    }

    public boolean createMilestone(CareerMilestone m) {
        String sql = "INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, " +
                     "salary_range, skills_required, milestone_description, recommended_action) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, m.getPathId());
            ps.setInt(2, m.getLevelOrder());
            ps.setString(3, m.getLevelName());
            ps.setString(4, m.getExperienceYearsRange());
            ps.setString(5, m.getSalaryRange());
            ps.setString(6, m.getSkillsRequired());
            ps.setString(7, m.getMilestoneDescription());
            ps.setString(8, m.getRecommendedAction());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        m.setMilestoneId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[CareerPathDAO.createMilestone] Error: " + e.getMessage());
        }
        return false;
    }

    private CareerPath mapCareerPath(ResultSet rs) throws SQLException {
        CareerPath cp = new CareerPath();
        cp.setPathId(rs.getInt("path_id"));
        cp.setTitle(rs.getString("title"));
        cp.setCategory(rs.getString("category"));
        cp.setDescription(rs.getString("description"));
        cp.setRequiredCoreSkills(rs.getString("required_core_skills"));
        cp.setMinStartingExperienceYears(rs.getInt("min_starting_experience_years"));
        cp.setAverageMarketSalary(rs.getString("average_market_salary"));
        cp.setCreatedAt(rs.getTimestamp("created_at"));
        return cp;
    }

    private CareerMilestone mapCareerMilestone(ResultSet rs) throws SQLException {
        CareerMilestone cm = new CareerMilestone();
        cm.setMilestoneId(rs.getInt("milestone_id"));
        cm.setPathId(rs.getInt("path_id"));
        cm.setLevelOrder(rs.getInt("level_order"));
        cm.setLevelName(rs.getString("level_name"));
        cm.setExperienceYearsRange(rs.getString("experience_years_range"));
        cm.setSalaryRange(rs.getString("salary_range"));
        cm.setSkillsRequired(rs.getString("skills_required"));
        cm.setMilestoneDescription(rs.getString("milestone_description"));
        cm.setRecommendedAction(rs.getString("recommended_action"));
        return cm;
    }
}
