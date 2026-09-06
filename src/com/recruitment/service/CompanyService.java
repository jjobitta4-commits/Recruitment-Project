package com.recruitment.service;

import com.recruitment.dao.CompanyDAO;
import com.recruitment.model.Company;

import java.util.List;

/**
 * Service layer for Enterprise/Company profile management and Admin verification.
 */
public class CompanyService {

    private final CompanyDAO companyDAO = new CompanyDAO();

    public List<Company> getApprovedCompanies() {
        return companyDAO.getApprovedCompanies();
    }

    public List<Company> getAllCompanies() {
        return companyDAO.getAllCompanies();
    }

    public List<Company> getPendingCompanies() {
        return companyDAO.getPendingCompanies();
    }

    public Company getCompanyById(int companyId) {
        return companyDAO.getCompanyById(companyId);
    }

    public Company getOrCreateCompany(String name, String industry, String location) {
        if (name == null || name.trim().isEmpty()) {
            name = "Tech Enterprise";
        }
        return companyDAO.getOrCreateCompany(name, industry, location);
    }

    public boolean registerCompany(Company company) {
        if (company == null || company.getName() == null || company.getName().trim().isEmpty()) {
            return false;
        }
        int id = companyDAO.createCompany(company);
        return id > 0;
    }

    public boolean approveCompany(int companyId) {
        return companyDAO.updateApprovalStatus(companyId, "approved");
    }

    public boolean rejectCompany(int companyId) {
        return companyDAO.updateApprovalStatus(companyId, "rejected");
    }

    public boolean updateCompany(Company company) {
        if (company == null || company.getCompanyId() <= 0) return false;
        return companyDAO.updateCompany(company);
    }
}
