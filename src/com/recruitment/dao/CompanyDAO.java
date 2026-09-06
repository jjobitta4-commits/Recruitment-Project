package com.recruitment.dao;

import com.recruitment.model.Company;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Company entity.
 * Supports CRUD, enterprise registration, and Admin moderation workflows.
 */
public class CompanyDAO {

    /**
     * Creates a new company record.
     * @return generated company_id or -1 on failure
     */
    public int createCompany(Company c) {
        String sql = "INSERT INTO companies (name, description, industry, website, location, approval_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getIndustry());
            ps.setString(4, c.getWebsite());
            ps.setString(5, c.getLocation());
            ps.setString(6, c.getApprovalStatus() != null ? c.getApprovalStatus() : "approved");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        c.setCompanyId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.createCompany] Error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves a company by its ID.
     */
    public Company getCompanyById(int companyId) {
        String sql = "SELECT * FROM companies WHERE company_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCompany(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.getCompanyById] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a company by its exact name.
     */
    public Company getCompanyByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT * FROM companies WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCompany(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.getCompanyByName] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves or creates a company by name.
     */
    public Company getOrCreateCompany(String name, String industry, String location) {
        if (name == null || name.trim().isEmpty()) {
            name = "Default Enterprise";
        }
        Company existing = getCompanyByName(name);
        if (existing != null) {
            return existing;
        }

        Company c = new Company();
        c.setName(name.trim());
        c.setIndustry(industry != null ? industry : "Technology");
        c.setLocation(location != null ? location : "Remote / Global");
        c.setApprovalStatus("approved");
        int id = createCompany(c);
        if (id > 0) {
            c.setCompanyId(id);
            return c;
        }
        return null;
    }

    /**
     * Retrieves all approved companies for recruiter selection.
     */
    public List<Company> getApprovedCompanies() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies WHERE approval_status = 'approved' ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCompany(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.getApprovedCompanies] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all companies across the platform for Admin management.
     */
    public List<Company> getAllCompanies() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCompany(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.getAllCompanies] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves companies awaiting Admin approval.
     */
    public List<Company> getPendingCompanies() {
        List<Company> list = new ArrayList<>();
        String sql = "SELECT * FROM companies WHERE approval_status = 'pending' ORDER BY created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCompany(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.getPendingCompanies] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates company moderation status (approved/rejected).
     */
    public boolean updateApprovalStatus(int companyId, String status) {
        String sql = "UPDATE companies SET approval_status = ? WHERE company_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, companyId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.updateApprovalStatus] Error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates an existing company's profile information.
     */
    public boolean updateCompany(Company c) {
        String sql = "UPDATE companies SET name = ?, description = ?, industry = ?, website = ?, location = ? " +
                "WHERE company_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getIndustry());
            ps.setString(4, c.getWebsite());
            ps.setString(5, c.getLocation());
            ps.setInt(6, c.getCompanyId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CompanyDAO.updateCompany] Error: " + e.getMessage());
        }
        return false;
    }

    private Company mapCompany(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setCompanyId(rs.getInt("company_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setIndustry(rs.getString("industry"));
        c.setWebsite(rs.getString("website"));
        c.setLocation(rs.getString("location"));
        c.setApprovalStatus(rs.getString("approval_status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
