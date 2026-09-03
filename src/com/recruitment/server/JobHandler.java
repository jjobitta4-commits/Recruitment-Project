package com.recruitment.server;

import com.recruitment.dao.JobDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.model.Job;
import com.recruitment.model.Recruiter;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Job operations (Listing, Searching, Creating, Updating, Deleting).
 */
public class JobHandler implements HttpHandler {

    private final JobDAO jobDAO = new JobDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET":
                    if (path.endsWith("/recruiter")) {
                        handleGetRecruiterJobs(exchange);
                    } else {
                        handleGetJobs(exchange);
                    }
                    break;
                case "POST":
                    handleCreateJob(exchange);
                    break;
                case "PUT":
                    if (path.endsWith("/status")) {
                        handleUpdateJobStatus(exchange);
                    } else {
                        handleUpdateJob(exchange);
                    }
                    break;
                case "DELETE":
                    handleDeleteJob(exchange);
                    break;
                default:
                    ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (Exception e) {
            System.err.println("[JobHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error processing job request.");
        }
    }

    private void handleGetJobs(HttpExchange exchange) throws IOException {
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());

        // Check if single job ID requested
        if (query.containsKey("id")) {
            try {
                int jobId = Integer.parseInt(query.get("id"));
                Job job = jobDAO.getJobById(jobId);
                if (job != null) {
                    ResponseHelper.sendSuccess(exchange, "Job retrieved successfully", job);
                } else {
                    ResponseHelper.sendError(exchange, 404, "Job not found.");
                }
            } catch (NumberFormatException e) {
                ResponseHelper.sendError(exchange, 400, "Invalid Job ID format.");
            }
            return;
        }

        // Filter / Search jobs
        String keyword = query.get("keyword");
        String jobType = query.get("jobType");
        String country = query.get("country");
        String location = query.get("location");
        String skill = query.get("skill");
        String experience = query.get("experience");

        List<Job> jobs;
        if (keyword != null || jobType != null || country != null || location != null || skill != null || experience != null) {
            jobs = jobDAO.searchJobs(keyword, jobType, country, location, skill, experience);
        } else {
            jobs = jobDAO.getAllActiveJobs();
        }

        ResponseHelper.sendSuccess(exchange, "Jobs retrieved successfully", jobs);
    }

    private void handleGetRecruiterJobs(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Recruiter authentication required.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        if (recruiterId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
            return;
        }

        List<Job> jobs = jobDAO.getJobsByRecruiter(recruiterId);
        ResponseHelper.sendSuccess(exchange, "Recruiter jobs retrieved", jobs);
    }

    private void handleCreateJob(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can post jobs.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        if (recruiterId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Recruiter profile not found.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        String title = JSONUtil.getString(data, "title", "").trim();
        String description = JSONUtil.getString(data, "description", "").trim();
        String skillsRequired = JSONUtil.getString(data, "skillsRequired", "").trim();
        String jobType = JSONUtil.getString(data, "jobType", "Full Time").trim();

        if (title.isEmpty() || description.isEmpty() || skillsRequired.isEmpty()) {
            ResponseHelper.sendError(exchange, 400, "Job Title, Description, and Skills are required fields.");
            return;
        }

        Recruiter recruiter = recruiterDAO.getRecruiterById(recruiterId);
        String companyName = recruiter != null ? recruiter.getCompanyName() : "Company";

        Job job = new Job();
        job.setRecruiterId(recruiterId);
        job.setTitle(title);
        job.setCompany(JSONUtil.getString(data, "company", companyName));
        job.setDescription(description);
        job.setSkillsRequired(skillsRequired);
        job.setEducationRequired(JSONUtil.getString(data, "educationRequired", ""));
        job.setExperienceRequired(JSONUtil.getString(data, "experienceRequired", ""));
        job.setSalaryRange(JSONUtil.getString(data, "salaryRange", ""));
        job.setJobType(jobType);
        job.setLocation(JSONUtil.getString(data, "location", ""));
        job.setCountry(JSONUtil.getString(data, "country", ""));
        job.setVacancies(JSONUtil.getInt(data, "vacancies", 1));
        job.setDeadline(JSONUtil.getDateOrNull(data, "deadline"));
        job.setStatus("Active");

        boolean success = jobDAO.createJob(job);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job posted successfully!", job);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to create job posting.");
        }
    }

    private void handleUpdateJob(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can update jobs.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int jobId = JSONUtil.getInt(data, "jobId", 0);
        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Valid jobId is required.");
            return;
        }

        Job existing = jobDAO.getJobById(jobId);
        if (existing == null) {
            ResponseHelper.sendError(exchange, 404, "Job not found.");
            return;
        }

        if (existing.getRecruiterId() != recruiterId) {
            ResponseHelper.sendError(exchange, 403, "You do not have permission to modify this job posting.");
            return;
        }

        existing.setTitle(JSONUtil.getString(data, "title", existing.getTitle()));
        existing.setCompany(JSONUtil.getString(data, "company", existing.getCompany()));
        existing.setDescription(JSONUtil.getString(data, "description", existing.getDescription()));
        existing.setSkillsRequired(JSONUtil.getString(data, "skillsRequired", existing.getSkillsRequired()));
        existing.setEducationRequired(JSONUtil.getString(data, "educationRequired", existing.getEducationRequired()));
        existing.setExperienceRequired(JSONUtil.getString(data, "experienceRequired", existing.getExperienceRequired()));
        existing.setSalaryRange(JSONUtil.getString(data, "salaryRange", existing.getSalaryRange()));
        existing.setJobType(JSONUtil.getString(data, "jobType", existing.getJobType()));
        existing.setLocation(JSONUtil.getString(data, "location", existing.getLocation()));
        existing.setCountry(JSONUtil.getString(data, "country", existing.getCountry()));
        existing.setVacancies(JSONUtil.getInt(data, "vacancies", existing.getVacancies()));
        if (data.containsKey("deadline")) {
            existing.setDeadline(JSONUtil.getDateOrNull(data, "deadline"));
        }
        if (data.containsKey("status")) {
            existing.setStatus(JSONUtil.getString(data, "status", existing.getStatus()));
        }

        boolean success = jobDAO.updateJob(existing);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job updated successfully", existing);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to update job posting.");
        }
    }

    private void handleUpdateJobStatus(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can update job status.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> data = JSONUtil.parseObject(body);

        int jobId = JSONUtil.getInt(data, "jobId", 0);
        String status = JSONUtil.getString(data, "status", "Active");

        Job existing = jobDAO.getJobById(jobId);
        if (existing == null) {
            ResponseHelper.sendError(exchange, 404, "Job not found.");
            return;
        }

        if (existing.getRecruiterId() != recruiterId) {
            ResponseHelper.sendError(exchange, 403, "You do not have permission to modify this job.");
            return;
        }

        boolean success = jobDAO.updateJobStatus(jobId, status);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job status changed to " + status);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to change job status.");
        }
    }

    private void handleDeleteJob(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isRecruiter()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Only recruiters can delete jobs.");
            return;
        }

        int recruiterId = getOrResolveRecruiterId(session);
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
        int jobId = 0;
        if (query.containsKey("id")) {
            try { jobId = Integer.parseInt(query.get("id")); } catch (Exception ignored) {}
        }

        if (jobId <= 0) {
            ResponseHelper.sendError(exchange, 400, "Missing or invalid Job ID.");
            return;
        }

        Job existing = jobDAO.getJobById(jobId);
        if (existing == null) {
            ResponseHelper.sendError(exchange, 404, "Job not found.");
            return;
        }

        if (existing.getRecruiterId() != recruiterId) {
            ResponseHelper.sendError(exchange, 403, "You do not have permission to delete this job.");
            return;
        }

        boolean success = jobDAO.deleteJob(jobId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job deleted successfully.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to delete job.");
        }
    }

    private int getOrResolveRecruiterId(SessionManager.UserSession session) {
        if (session.getRecruiterId() != null && session.getRecruiterId() > 0) {
            return session.getRecruiterId();
        }
        Recruiter r = recruiterDAO.getRecruiterByUserId(session.getUserId());
        if (r != null) {
            session.setRecruiterId(r.getRecruiterId());
            return r.getRecruiterId();
        }
        return -1;
    }
}
