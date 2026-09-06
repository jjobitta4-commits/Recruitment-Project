package com.recruitment.server;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.ApplicationDAO;
import com.recruitment.dao.InterviewDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Application;
import com.recruitment.model.Interview;
import com.recruitment.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP Handler for Applicant Profile and Resume Management.
 */
public class ApplicantHandler implements HttpHandler {

    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final com.recruitment.dao.CandidateDAO candidateDAO = new com.recruitment.dao.CandidateDAO();
    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final InterviewDAO interviewDAO = new InterviewDAO();
    private final String uploadsDir;

    public ApplicantHandler(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null || !session.isApplicant()) {
            ResponseHelper.sendError(exchange, 403, "Access denied. Applicant authorization required.");
            return;
        }

        int applicantId = getOrResolveApplicantId(session);
        int candidateId = getOrResolveCandidateId(session);
        if (applicantId <= 0 && candidateId <= 0) {
            ResponseHelper.sendError(exchange, 404, "Applicant profile not found.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.endsWith("/stats") && "GET".equals(method)) {
                List<Application> apps = (candidateId > 0) ? applicationDAO.getApplicationsByCandidate(candidateId)
                        : applicationDAO.getApplicationsByApplicant(applicantId);
                List<Interview> ivs = (candidateId > 0) ? interviewDAO.getInterviewsByApplicant(candidateId)
                        : interviewDAO.getInterviewsByApplicant(applicantId);

                int shortlisted = 0;
                int selected = 0;
                for (Application a : apps) {
                    String st = a.getStatus();
                    if (st != null) {
                        if ("Shortlisted".equalsIgnoreCase(st) || "Interview_Scheduled".equalsIgnoreCase(st) || "Interview Scheduled".equalsIgnoreCase(st)) {
                            shortlisted++;
                        }
                        if ("Selected".equalsIgnoreCase(st) || "Offer".equalsIgnoreCase(st) || "Offered".equalsIgnoreCase(st)) {
                            selected++;
                        }
                    }
                }

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalApplications", apps.size());
                stats.put("shortlistedCount", shortlisted);
                stats.put("selectedCount", selected);
                stats.put("interviewsCount", ivs.size());

                ResponseHelper.sendSuccess(exchange, "Applicant dashboard stats", stats);

            } else if (path.endsWith("/profile") && "GET".equals(method)) {
                Applicant ap = applicantDAO.getApplicantById(applicantId);
                ResponseHelper.sendSuccess(exchange, "Applicant profile retrieved", ap);

            } else if (path.endsWith("/profile") && "PUT".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                Applicant ap = applicantDAO.getApplicantById(applicantId);
                if (ap == null) {
                    ResponseHelper.sendError(exchange, 404, "Applicant not found.");
                    return;
                }

                ap.setFullName(JSONUtil.getString(data, "fullName", ap.getFullName()));
                ap.setPhone(JSONUtil.getString(data, "phone", ap.getPhone()));
                if (data.containsKey("dob")) {
                    ap.setDob(JSONUtil.getDateOrNull(data, "dob"));
                }
                ap.setGender(JSONUtil.getString(data, "gender", ap.getGender()));
                ap.setAddress(JSONUtil.getString(data, "address", ap.getAddress()));
                ap.setCity(JSONUtil.getString(data, "city", ap.getCity()));
                ap.setCountry(JSONUtil.getString(data, "country", ap.getCountry()));
                ap.setEducation(JSONUtil.getString(data, "education", ap.getEducation()));
                ap.setUniversity(JSONUtil.getString(data, "university", ap.getUniversity()));
                if (data.containsKey("graduationYear")) {
                    ap.setGraduationYear(JSONUtil.getIntegerOrNull(data, "graduationYear"));
                }
                ap.setSkills(JSONUtil.getString(data, "skills", ap.getSkills()));
                ap.setExperienceYears(JSONUtil.getInt(data, "experienceYears", ap.getExperienceYears()));
                ap.setExpectedSalary(JSONUtil.getString(data, "expectedSalary", ap.getExpectedSalary()));
                ap.setLinkedinUrl(JSONUtil.getString(data, "linkedinUrl", ap.getLinkedinUrl()));
                ap.setGithubUrl(JSONUtil.getString(data, "githubUrl", ap.getGithubUrl()));
                ap.setLeetcodeUrl(JSONUtil.getString(data, "leetcodeUrl", ap.getLeetcodeUrl()));

                boolean success = applicantDAO.updateApplicantProfile(ap);
                if (success) {
                    ResponseHelper.sendSuccess(exchange, "Profile updated successfully", ap);
                } else {
                    ResponseHelper.sendError(exchange, 500, "Failed to update profile.");
                }

            } else if (path.endsWith("/resume") && "POST".equals(method)) {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().contains("multipart/form-data")) {
                    ResponseHelper.sendError(exchange, 400, "Expected multipart/form-data request.");
                    return;
                }

                byte[] bodyBytes = ResponseHelper.readBodyBytes(exchange);
                MultipartParser.MultipartResult mpResult = MultipartParser.parse(contentType, bodyBytes);
                MultipartParser.FileItem file = mpResult.getFirstFile();

                if (file == null || file.getContent() == null || file.getContent().length == 0) {
                    ResponseHelper.sendError(exchange, 400, "No file uploaded or file is empty.");
                    return;
                }

                String originalName = file.getFileName();
                if (!originalName.toLowerCase().endsWith(".pdf")) {
                    ResponseHelper.sendError(exchange, 400, "Only PDF files (.pdf) are allowed.");
                    return;
                }

                File resumeFolder = new File(uploadsDir, "resumes");
                if (!resumeFolder.exists()) {
                    resumeFolder.mkdirs();
                }
                String safeName = "resume_" + UUID.randomUUID().toString().substring(0, 8) + "_" +
                        originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File dest = new File(resumeFolder, safeName);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(file.getContent());
                }

                applicantDAO.updateResumeFile(applicantId, safeName);
                Map<String, String> res = new HashMap<>();
                res.put("resumeFile", safeName);
                res.put("resumeUrl", "/uploads/resumes/" + safeName);
                ResponseHelper.sendSuccess(exchange, "Resume uploaded successfully!", res);

            } else {
                ResponseHelper.sendError(exchange, 404, "Unknown applicant API endpoint.");
            }
        } catch (Exception e) {
            System.err.println("[ApplicantHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal error during applicant operation.");
        }
    }

    private int getOrResolveApplicantId(SessionManager.UserSession session) {
        if (session.getApplicantId() != null && session.getApplicantId() > 0) {
            return session.getApplicantId();
        }
        Applicant ap = applicantDAO.getApplicantByUserId(session.getUserId());
        if (ap != null) {
            session.setApplicantId(ap.getApplicantId());
            return ap.getApplicantId();
        }
        return -1;
    }

    private int getOrResolveCandidateId(SessionManager.UserSession session) {
        if (session.getCandidateId() != null && session.getCandidateId() > 0) {
            return session.getCandidateId();
        }
        com.recruitment.model.Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
        if (c != null) {
            session.setCandidateId(c.getCandidateId());
            return c.getCandidateId();
        }
        return -1;
    }
}
