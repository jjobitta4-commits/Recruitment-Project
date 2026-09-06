package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.model.*;
import com.recruitment.service.CandidateService;
import com.recruitment.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.*;

/**
 * REST Handler for Candidate Profile Management, Skills Taxonomy,
 * Portfolio Entries, and the Resume Keyword Extractor.
 */
public class CandidateHandler implements HttpHandler {

    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final CandidateService candidateService = new CandidateService();
    private final String uploadsDir;

    public CandidateHandler(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) return;

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || (!session.isApplicant() && !"candidate".equalsIgnoreCase(session.getRole()))) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Candidate authorization required.");
            return;
        }

        int candidateId = resolveCandidateId(session);
        if (candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Candidate profile not found for active user.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            // Profile Details
            if (path.endsWith("/profile") && "GET".equals(method)) {
                Candidate c = candidateService.getCandidate(session.getUserId());
                ResponseHelper.sendSuccess(exchange, "Candidate profile retrieved", c);

            } else if (path.endsWith("/profile") && "PUT".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Candidate c = candidateService.getCandidate(session.getUserId());
                if (c == null) {
                    ResponseHelper.sendError(exchange, 404, "Candidate profile not found");
                    return;
                }

                c.setFullName(JSONUtil.getString(data, "fullName", c.getFullName()));
                c.setPhone(JSONUtil.getString(data, "phone", c.getPhone()));
                if (data.containsKey("dob")) {
                    c.setDob(JSONUtil.getDateOrNull(data, "dob"));
                }
                c.setGender(JSONUtil.getString(data, "gender", c.getGender()));
                c.setAddress(JSONUtil.getString(data, "address", c.getAddress()));
                c.setCity(JSONUtil.getString(data, "city", c.getCity()));
                c.setCountry(JSONUtil.getString(data, "country", c.getCountry()));
                c.setBio(JSONUtil.getString(data, "bio", c.getBio()));

                boolean ok = candidateService.updateProfile(c);
                if (ok) {
                    syncApplicantProfile(c);
                    ResponseHelper.sendSuccess(exchange, "Profile updated successfully!", c);
                } else {
                    ResponseHelper.sendError(exchange, 500, "Failed to update profile.");
                }

            // Skills Taxonomy & Declared Skills
            } else if (path.endsWith("/skills") && "GET".equals(method)) {
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("masterSkills", candidateDAO.getAllMasterSkills());
                resp.put("candidateSkills", candidateDAO.getSkillsByCandidateId(candidateId));
                ResponseHelper.sendSuccess(exchange, "Candidate skills loaded", resp);

            } else if (path.endsWith("/skills") && "POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                int skillId = JSONUtil.getInt(data, "skillId", 0);
                String level = JSONUtil.getString(data, "proficiencyLevel", "Intermediate");
                int years = JSONUtil.getInt(data, "yearsExperience", 1);

                if (skillId <= 0) {
                    ResponseHelper.sendError(exchange, 400, "Valid skillId is required.");
                    return;
                }

                boolean ok = candidateDAO.addCandidateSkill(candidateId, skillId, level, years);
                if (ok) {
                    Candidate c = candidateService.getCandidate(session.getUserId());
                    ResponseHelper.sendSuccess(exchange, "Skill added successfully", c != null ? c.getSkills() : null);
                } else {
                    ResponseHelper.sendError(exchange, 500, "Failed to save skill.");
                }

            } else if (path.endsWith("/skills") && "DELETE".equals(method)) {
                Map<String, String> q = ResponseHelper.parseQueryParams(exchange.getRequestURI().getQuery());
                int skillId = 0;
                try { skillId = Integer.parseInt(q.get("skillId")); } catch (Exception ignored) {}

                if (skillId <= 0) {
                    ResponseHelper.sendError(exchange, 400, "Valid skillId query parameter required.");
                    return;
                }
                boolean ok = candidateDAO.removeCandidateSkill(candidateId, skillId);
                ResponseHelper.sendSuccess(exchange, ok ? "Skill removed" : "Skill not found");

            // Education Management
            } else if (path.endsWith("/education") && "POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Education edu = new Education();
                edu.setCandidateId(candidateId);
                edu.setDegree(JSONUtil.getString(data, "degree", ""));
                edu.setInstitution(JSONUtil.getString(data, "institution", ""));
                edu.setFieldOfStudy(JSONUtil.getString(data, "fieldOfStudy", ""));
                edu.setStartYear(JSONUtil.getIntegerOrNull(data, "startYear"));
                edu.setEndYear(JSONUtil.getIntegerOrNull(data, "endYear"));
                edu.setGradeOrGpa(JSONUtil.getString(data, "gradeOrGpa", ""));

                boolean ok = candidateDAO.addEducation(edu);
                ResponseHelper.sendSuccess(exchange, ok ? "Education added successfully" : "Failed to add education", 
                        candidateDAO.getEducationByCandidateId(candidateId));

            } else if (path.endsWith("/education") && "DELETE".equals(method)) {
                Map<String, String> q = ResponseHelper.parseQueryParams(exchange.getRequestURI().getQuery());
                int eduId = Integer.parseInt(q.getOrDefault("educationId", "0"));
                boolean ok = candidateDAO.deleteEducation(eduId, candidateId);
                ResponseHelper.sendSuccess(exchange, ok ? "Education deleted" : "Failed to delete");

            // Experience Management
            } else if (path.endsWith("/experience") && "POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Experience exp = new Experience();
                exp.setCandidateId(candidateId);
                exp.setCompanyName(JSONUtil.getString(data, "companyName", ""));
                exp.setJobTitle(JSONUtil.getString(data, "jobTitle", ""));
                exp.setStartDate(JSONUtil.getDateOrNull(data, "startDate"));
                exp.setEndDate(JSONUtil.getDateOrNull(data, "endDate"));
                exp.setCurrent(Boolean.parseBoolean(JSONUtil.getString(data, "isCurrent", "false")));
                exp.setDescription(JSONUtil.getString(data, "description", ""));

                boolean ok = candidateDAO.addExperience(exp);
                ResponseHelper.sendSuccess(exchange, ok ? "Experience record added" : "Failed to add experience",
                        candidateDAO.getExperienceByCandidateId(candidateId));

            } else if (path.endsWith("/experience") && "DELETE".equals(method)) {
                Map<String, String> q = ResponseHelper.parseQueryParams(exchange.getRequestURI().getQuery());
                int expId = Integer.parseInt(q.getOrDefault("experienceId", "0"));
                boolean ok = candidateDAO.deleteExperience(expId, candidateId);
                ResponseHelper.sendSuccess(exchange, ok ? "Experience record deleted" : "Failed to delete");

            // Projects Management
            } else if (path.endsWith("/projects") && "POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Project proj = new Project();
                proj.setCandidateId(candidateId);
                proj.setTitle(JSONUtil.getString(data, "title", ""));
                proj.setTechnologiesUsed(JSONUtil.getString(data, "technologiesUsed", ""));
                proj.setProjectUrl(JSONUtil.getString(data, "projectUrl", ""));
                proj.setDescription(JSONUtil.getString(data, "description", ""));

                boolean ok = candidateDAO.addProject(proj);
                ResponseHelper.sendSuccess(exchange, ok ? "Project added successfully" : "Failed to add project",
                        candidateDAO.getProjectsByCandidateId(candidateId));

            } else if (path.endsWith("/projects") && "DELETE".equals(method)) {
                Map<String, String> q = ResponseHelper.parseQueryParams(exchange.getRequestURI().getQuery());
                int projId = Integer.parseInt(q.getOrDefault("projectId", "0"));
                boolean ok = candidateDAO.deleteProject(projId, candidateId);
                ResponseHelper.sendSuccess(exchange, ok ? "Project deleted" : "Failed to delete");

            // Resume Keyword Extractor (Raw Text Mode)
            } else if (path.endsWith("/parse-resume-text") && "POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                String resumeText = JSONUtil.getString(data, "resumeText", "");

                Map<String, Object> extracted = candidateService.extractKeywordsFromResume(resumeText);
                ResponseHelper.sendSuccess(exchange, "Resume keywords extracted successfully", extracted);

            // Resume File Upload Mode
            } else if (path.endsWith("/resume") && "POST".equals(method)) {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().contains("multipart/form-data")) {
                    ResponseHelper.sendError(exchange, 400, "Content-Type must be multipart/form-data");
                    return;
                }

                byte[] bodyBytes = ResponseHelper.readBodyBytes(exchange);
                MultipartParser.MultipartResult mp = MultipartParser.parse(contentType, bodyBytes);
                MultipartParser.FileItem file = mp.getFirstFile();

                if (file == null || file.getContent() == null || file.getContent().length == 0) {
                    ResponseHelper.sendError(exchange, 400, "No file uploaded.");
                    return;
                }

                String origName = file.getFileName();
                if (!origName.toLowerCase().endsWith(".pdf")) {
                    ResponseHelper.sendError(exchange, 400, "Only PDF (.pdf) format is supported.");
                    return;
                }

                File resumeFolder = new File(uploadsDir, "resumes");
                if (!resumeFolder.exists()) resumeFolder.mkdirs();
                String safeName = "resume_" + UUID.randomUUID().toString().substring(0, 8) + "_" +
                                  origName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File dest = new File(resumeFolder, safeName);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(file.getContent());
                }

                // Extract text and keywords
                String textContent = new String(file.getContent(), "ISO-8859-1"); // basic string extraction
                Map<String, Object> keywords = candidateService.extractKeywordsFromResume(textContent);
                String detectedJson = JSONUtil.toJson(keywords.get("detectedSkills"));

                candidateDAO.saveResume(candidateId, safeName, textContent, detectedJson);

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("fileName", safeName);
                resp.put("fileUrl", "/uploads/resumes/" + safeName);
                resp.put("extractedKeywords", keywords);

                ResponseHelper.sendSuccess(exchange, "Resume uploaded & analyzed successfully!", resp);

            } else {
                ResponseHelper.sendError(exchange, 404, "Endpoint not found: " + path);
            }

        } catch (Exception e) {
            System.err.println("[CandidateHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal error in candidate operations.");
        }
    }

    private int resolveCandidateId(SessionManager.UserSession session) {
        Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
        if (c != null) {
            session.setCandidateId(c.getCandidateId());
            return c.getCandidateId();
        }
        return -1;
    }

    private void syncApplicantProfile(Candidate c) {
        try {
            com.recruitment.dao.ApplicantDAO applicantDAO = new com.recruitment.dao.ApplicantDAO();
            com.recruitment.model.Applicant ap = applicantDAO.getApplicantByUserId(c.getUserId());
            if (ap != null) {
                ap.setFullName(c.getFullName());
                ap.setPhone(c.getPhone());
                ap.setDob(c.getDob());
                ap.setGender(c.getGender());
                ap.setAddress(c.getAddress());
                ap.setCity(c.getCity());
                ap.setCountry(c.getCountry());
                applicantDAO.updateApplicantProfile(ap);
            }
        } catch (Exception e) {
            System.err.println("[CandidateHandler.syncApplicantProfile] Warning: " + e.getMessage());
        }
    }
}
