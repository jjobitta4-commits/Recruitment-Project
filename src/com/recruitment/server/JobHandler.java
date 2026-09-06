package com.recruitment.server;

import com.recruitment.model.Job;
import com.recruitment.model.Recruiter;
import com.recruitment.service.JobService;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enhanced HTTP Handler for Job operations.
 * Routes job queries, dual-skill posting, status updates, and deletion through JobService.
 */
public class JobHandler implements HttpHandler {

    private final JobService jobService = new JobService();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final com.recruitment.dao.ApplicantDAO applicantDAO = new com.recruitment.dao.ApplicantDAO();

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
            ResponseHelper.sendError(exchange, 500, "Internal error processing job request: " + e.getMessage());
        }
    }

    private void handleGetJobs(HttpExchange exchange) throws IOException {
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        com.recruitment.model.Applicant currentApplicant = null;
        if (session != null && session.isApplicant()) {
            currentApplicant = applicantDAO.getApplicantByUserId(session.getUserId());
        }

        // Check if single job ID requested
        if (query.containsKey("id")) {
            try {
                int jobId = Integer.parseInt(query.get("id"));
                Job job = jobService.getJobById(jobId);
                if (job != null) {
                    if (currentApplicant != null) {
                        applyMatchScores(job, currentApplicant);
                    }
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
            jobs = jobService.searchJobs(keyword, jobType, country, location, skill, experience);
        } else {
            jobs = jobService.getAllActiveJobs();
        }

        if (currentApplicant != null) {
            for (Job j : jobs) {
                applyMatchScores(j, currentApplicant);
            }
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

        List<Job> jobs = jobService.getJobsByRecruiter(recruiterId);
        ResponseHelper.sendSuccess(exchange, "Recruiter jobs retrieved", jobs);
    }

    @SuppressWarnings("unchecked")
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
        String companyName = JSONUtil.getString(data, "company", "").trim();
        String jobType = JSONUtil.getString(data, "jobType", "Full Time").trim();
        String skillsRequired = JSONUtil.getString(data, "skillsRequired", "").trim();

        // Skill list payload: array of maps
        List<Map<String, Object>> skillInputs = new ArrayList<>();
        if (data.containsKey("skills") && data.get("skills") instanceof List) {
            List<?> rawList = (List<?>) data.get("skills");
            for (Object item : rawList) {
                if (item instanceof Map) {
                    skillInputs.add((Map<String, Object>) item);
                }
            }
        }

        if (title.isEmpty() || description.isEmpty() || (skillsRequired.isEmpty() && skillInputs.isEmpty())) {
            ResponseHelper.sendError(exchange, 400, "Job Title, Description, and Skills are required fields.");
            return;
        }

        Job job = new Job();
        job.setTitle(title);
        job.setDescription(description);
        job.setCompany(companyName);
        job.setCompanyId(JSONUtil.getInt(data, "companyId", 0));
        job.setJobType(jobType);
        job.setLocation(JSONUtil.getString(data, "location", ""));
        job.setCountry(JSONUtil.getString(data, "country", ""));
        job.setSalaryRange(JSONUtil.getString(data, "salaryRange", ""));
        job.setMinExperienceYears(JSONUtil.getInt(data, "minExperienceYears", 0));
        job.setMinEducation(JSONUtil.getString(data, "minEducation", JSONUtil.getString(data, "educationRequired", "")));
        job.setVacancies(JSONUtil.getInt(data, "vacancies", 1));
        job.setDeadline(JSONUtil.getDateOrNull(data, "deadline"));
        job.setStatus("Active");
        // Optional approval status parameter, default to approved or pending
        job.setApprovalStatus(JSONUtil.getString(data, "approvalStatus", "approved"));
        job.setSkillsRequired(skillsRequired);

        boolean success = jobService.createJobPosting(job, skillInputs, recruiterId, companyName);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job opening published successfully!", job);
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to create job posting.");
        }
    }

    @SuppressWarnings("unchecked")
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

        Job job = jobService.getJobById(jobId);
        if (job == null) {
            ResponseHelper.sendError(exchange, 404, "Job not found.");
            return;
        }

        if (job.getRecruiterId() != recruiterId) {
            ResponseHelper.sendError(exchange, 403, "You do not have permission to modify this job posting.");
            return;
        }

        job.setTitle(JSONUtil.getString(data, "title", job.getTitle()));
        job.setDescription(JSONUtil.getString(data, "description", job.getDescription()));
        job.setJobType(JSONUtil.getString(data, "jobType", job.getJobType()));
        job.setLocation(JSONUtil.getString(data, "location", job.getLocation()));
        job.setCountry(JSONUtil.getString(data, "country", job.getCountry()));
        job.setSalaryRange(JSONUtil.getString(data, "salaryRange", job.getSalaryRange()));
        job.setMinExperienceYears(JSONUtil.getInt(data, "minExperienceYears", job.getMinExperienceYears()));
        job.setMinEducation(JSONUtil.getString(data, "minEducation", job.getMinEducation()));
        job.setVacancies(JSONUtil.getInt(data, "vacancies", job.getVacancies()));
        if (data.containsKey("deadline")) {
            job.setDeadline(JSONUtil.getDateOrNull(data, "deadline"));
        }
        if (data.containsKey("status")) {
            job.setStatus(JSONUtil.getString(data, "status", job.getStatus()));
        }

        List<Map<String, Object>> skillInputs = new ArrayList<>();
        if (data.containsKey("skills") && data.get("skills") instanceof List) {
            List<?> rawList = (List<?>) data.get("skills");
            for (Object item : rawList) {
                if (item instanceof Map) {
                    skillInputs.add((Map<String, Object>) item);
                }
            }
        }

        boolean success = jobService.updateJobPosting(job, skillInputs, recruiterId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job updated successfully", job);
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

        boolean success = jobService.updateJobStatus(jobId, status, recruiterId);
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

        boolean success = jobService.deleteJob(jobId, recruiterId);
        if (success) {
            ResponseHelper.sendSuccess(exchange, "Job opening deleted successfully.");
        } else {
            ResponseHelper.sendError(exchange, 500, "Failed to delete job.");
        }
    }

    private void applyMatchScores(Job job, com.recruitment.model.Applicant currentApplicant) {
        Map<String, Object> match = com.recruitment.util.SkillMatcher.calculateMatch(
                currentApplicant.getSkills(), currentApplicant.getExperienceYears(),
                job.getSkillsRequired(), job.getExperienceRequired());
        job.setMatchScore((Integer) match.get("score"));
        job.setMatchLevel((String) match.get("level"));
        @SuppressWarnings("unchecked")
        List<String> matched = (List<String>) match.get("matchedSkills");
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) match.get("missingSkills");
        job.setMatchedSkills(matched);
        job.setMissingSkills(missing);
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
