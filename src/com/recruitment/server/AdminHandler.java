package com.recruitment.server;

import com.recruitment.model.Company;
import com.recruitment.model.Job;
import com.recruitment.service.CompanyService;
import com.recruitment.service.JobService;
import com.recruitment.util.DBConnection;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Platform Administrator Operations.
 * Handles job vacancy moderation, company vetting, and system stats.
 */
public class AdminHandler implements HttpHandler {

    private final JobService jobService = new JobService();
    private final CompanyService companyService = new CompanyService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isAdmin()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Administrator privileges required.");
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.endsWith("/jobs/pending")) {
                    handleGetPendingJobs(exchange);
                } else if (path.endsWith("/jobs")) {
                    handleGetAllJobs(exchange);
                } else if (path.endsWith("/companies/pending")) {
                    handleGetPendingCompanies(exchange);
                } else if (path.endsWith("/companies")) {
                    handleGetAllCompanies(exchange);
                } else if (path.endsWith("/stats")) {
                    handleGetAdminStats(exchange);
                } else {
                    ResponseHelper.sendError(exchange, 404, "Endpoint not found under /api/admin");
                }
            } else if ("POST".equals(method)) {
                if (path.endsWith("/jobs/approve")) {
                    handleApproveJob(exchange);
                } else if (path.endsWith("/jobs/reject")) {
                    handleRejectJob(exchange);
                } else if (path.endsWith("/companies/approve")) {
                    handleApproveCompany(exchange);
                } else if (path.endsWith("/companies/reject")) {
                    handleRejectCompany(exchange);
                } else {
                    ResponseHelper.sendError(exchange, 404, "Endpoint not found under /api/admin");
                }
            } else {
                ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("[AdminHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error in Admin Handler: " + e.getMessage());
        }
    }

    private void handleGetAllJobs(HttpExchange exchange) throws IOException {
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
        String filter = query.get("filter");
        List<Job> jobs = jobService.getAllJobsForAdmin(filter);
        ResponseHelper.sendSuccess(exchange, "Admin jobs retrieved", jobs);
    }

    private void handleGetPendingJobs(HttpExchange exchange) throws IOException {
        List<Job> jobs = jobService.getPendingJobsForAdmin();
        ResponseHelper.sendSuccess(exchange, "Pending jobs retrieved", jobs);
    }

    private void handleApproveJob(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int jobId = JSONUtil.getInt(data, "jobId", 0);

        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid jobId is required.");
            return;
        }

        boolean success = jobService.approveJob(jobId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job vacancy #" + jobId + " approved successfully.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to approve job vacancy.");
        }
    }

    private void handleRejectJob(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int jobId = JSONUtil.getInt(data, "jobId", 0);

        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid jobId is required.");
            return;
        }

        boolean success = jobService.rejectJob(jobId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job vacancy #" + jobId + " rejected.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to reject job vacancy.");
        }
    }

    private void handleGetAllCompanies(HttpExchange exchange) throws IOException {
        List<Company> companies = companyService.getAllCompanies();
        ResponseHelper.sendSuccess(exchange, "Companies retrieved", companies);
    }

    private void handleGetPendingCompanies(HttpExchange exchange) throws IOException {
        List<Company> companies = companyService.getPendingCompanies();
        ResponseHelper.sendSuccess(exchange, "Pending companies retrieved", companies);
    }

    private void handleApproveCompany(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int companyId = JSONUtil.getInt(data, "companyId", 0);

        if (companyId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid companyId is required.");
            return;
        }

        boolean success = companyService.approveCompany(companyId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Company #" + companyId + " approved.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to approve company.");
        }
    }

    private void handleRejectCompany(HttpExchange exchange) throws IOException {
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);
        int companyId = JSONUtil.getInt(data, "companyId", 0);

        if (companyId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid companyId is required.");
            return;
        }

        boolean success = companyService.rejectCompany(companyId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Company #" + companyId + " rejected.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to reject company.");
        }
    }

    private void handleGetAdminStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM jobs) AS total_jobs, " +
                "(SELECT COUNT(*) FROM jobs WHERE approval_status = 'pending') AS pending_jobs, " +
                "(SELECT COUNT(*) FROM jobs WHERE approval_status = 'approved') AS approved_jobs, " +
                "(SELECT COUNT(*) FROM companies) AS total_companies, " +
                "(SELECT COUNT(*) FROM recruiters) AS total_recruiters, " +
                "(SELECT COUNT(*) FROM candidates) AS total_candidates, " +
                "(SELECT COUNT(*) FROM applications) AS total_apps";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("totalJobs", rs.getInt("total_jobs"));
                stats.put("pendingJobs", rs.getInt("pending_jobs"));
                stats.put("approvedJobs", rs.getInt("approved_jobs"));
                stats.put("totalCompanies", rs.getInt("total_companies"));
                stats.put("totalRecruiters", rs.getInt("total_recruiters"));
                stats.put("totalCandidates", rs.getInt("total_candidates"));
                stats.put("totalApplications", rs.getInt("total_apps"));
            }
        } catch (Exception e) {
            System.err.println("[AdminHandler.getAdminStats] Error: " + e.getMessage());
        }

        ResponseHelper.sendSuccess(exchange, "Admin platform stats", stats);
    }
}
