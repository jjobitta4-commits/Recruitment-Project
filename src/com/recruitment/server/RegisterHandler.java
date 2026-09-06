package com.recruitment.server;

import com.recruitment.dao.ApplicantDAO;
import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.NotificationDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.dao.UserDAO;
import com.recruitment.model.Applicant;
import com.recruitment.model.Candidate;
import com.recruitment.model.Recruiter;
import com.recruitment.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP Handler for User and Profile Registration (Applicant & Recruiter).
 */
public class RegisterHandler implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();
    private final ApplicantDAO applicantDAO = new ApplicantDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final String uploadsDir;

    public RegisterHandler(String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            Map<String, Object> fields = new HashMap<>();
            String resumeFileName = null;

            if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
                byte[] bodyBytes = ResponseHelper.readBodyBytes(exchange);
                MultipartParser.MultipartResult mpResult = MultipartParser.parse(contentType, bodyBytes);
                fields.putAll(mpResult.getFields());

                MultipartParser.FileItem file = mpResult.getFirstFile();
                if (file != null && file.getContent() != null && file.getContent().length > 0) {
                    String originalName = file.getFileName();
                    if (!originalName.toLowerCase().endsWith(".pdf")) {
                        ResponseHelper.sendError(exchange, 400, "Only PDF format (.pdf) is supported for resumes.");
                        return;
                    }

                    // Save file to uploads/resumes/
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
                    resumeFileName = safeName;
                }
            } else {
                String body = ResponseHelper.readBody(exchange);
                fields = JSONUtil.parseObject(body);
            }

            String role = JSONUtil.getString(fields, "role", "applicant").trim().toLowerCase();
            String email = JSONUtil.getString(fields, "email", "").trim().toLowerCase();
            String password = JSONUtil.getString(fields, "password", "").trim();

            if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
                ResponseHelper.sendError(exchange, 400, "Please provide a valid email address.");
                return;
            }

            if (password.length() < 4) {
                ResponseHelper.sendError(exchange, 400, "Password must be at least 4 characters long.");
                return;
            }

            if (userDAO.isEmailTaken(email)) {
                ResponseHelper.sendError(exchange, 409, "Email is already registered. Please login instead.");
                return;
            }

            // Enforce admin exclusivity: Admin role cannot be self-registered and is locked to admin@recruithub.com
            if ("admin".equalsIgnoreCase(role)) {
                ResponseHelper.sendError(exchange, 403, "Administrator accounts cannot be self-registered. The admin role is restricted exclusively to admin@recruithub.com.");
                return;
            }

            if ("admin@recruithub.com".equalsIgnoreCase(email)) {
                ResponseHelper.sendError(exchange, 400, "The email 'admin@recruithub.com' is exclusively reserved for the platform administrator.");
                return;
            }

            if (!"candidate".equalsIgnoreCase(role) && !"applicant".equalsIgnoreCase(role) && !"recruiter".equalsIgnoreCase(role)) {
                ResponseHelper.sendError(exchange, 400, "Invalid registration role. Only 'candidate' and 'recruiter' roles are permitted.");
                return;
            }

            // Register User
            int userId = userDAO.registerUser(email, password, role);
            if (userId <= 0) {
                ResponseHelper.sendError(exchange, 500, "Failed to create user account.");
                return;
            }

            if ("applicant".equalsIgnoreCase(role) || "candidate".equalsIgnoreCase(role)) {
                Applicant ap = new Applicant();
                ap.setUserId(userId);
                ap.setFullName(JSONUtil.getString(fields, "fullName", "Applicant"));
                ap.setPhone(JSONUtil.getString(fields, "phone", ""));
                ap.setDob(JSONUtil.getDateOrNull(fields, "dob"));
                ap.setGender(JSONUtil.getString(fields, "gender", ""));
                ap.setAddress(JSONUtil.getString(fields, "address", ""));
                ap.setCity(JSONUtil.getString(fields, "city", ""));
                ap.setCountry(JSONUtil.getString(fields, "country", ""));
                ap.setEducation(JSONUtil.getString(fields, "education", ""));
                ap.setUniversity(JSONUtil.getString(fields, "university", ""));
                ap.setGraduationYear(JSONUtil.getIntegerOrNull(fields, "graduationYear"));
                ap.setSkills(JSONUtil.getString(fields, "skills", ""));
                ap.setExperienceYears(JSONUtil.getInt(fields, "experienceYears", 0));
                ap.setExpectedSalary(JSONUtil.getString(fields, "expectedSalary", ""));
                ap.setLinkedinUrl(JSONUtil.getString(fields, "linkedinUrl", ""));
                ap.setGithubUrl(JSONUtil.getString(fields, "githubUrl", ""));
                ap.setLeetcodeUrl(JSONUtil.getString(fields, "leetcodeUrl", ""));
                ap.setResumeFile(resumeFileName != null ? resumeFileName : JSONUtil.getString(fields, "resumeFile", null));

                boolean success = applicantDAO.createApplicant(ap);
                if (!success) {
                    ResponseHelper.sendError(exchange, 500, "Account created, but failed to save applicant profile.");
                    return;
                }

                // Provision normalized candidate profile
                Candidate cand = new Candidate();
                cand.setUserId(userId);
                cand.setFullName(ap.getFullName());
                cand.setPhone(ap.getPhone());
                cand.setDob(ap.getDob());
                cand.setGender(ap.getGender());
                cand.setAddress(ap.getAddress());
                cand.setCity(ap.getCity());
                cand.setCountry(ap.getCountry());
                cand.setBio("");
                cand.setProfileCompletion(20);
                candidateDAO.createCandidate(cand);

                // Send welcome notification
                notificationDAO.createNotification(userId, "Welcome to Recruitment Portal",
                        "Your applicant account has been created successfully! Complete your profile and browse open vacancies.");

                SessionManager.UserSession session = SessionManager.createSession(
                        userId, email, role, ap.getApplicantId(), null, cand.getCandidateId());

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("token", session.getToken());
                resp.put("userId", userId);
                resp.put("email", email);
                resp.put("role", role);
                resp.put("name", ap.getFullName());
                resp.put("applicantId", ap.getApplicantId());
                resp.put("candidateId", cand.getCandidateId());

                ResponseHelper.sendSuccess(exchange, "Applicant registered successfully!", resp);

            } else if ("recruiter".equalsIgnoreCase(role)) {
                Recruiter rc = new Recruiter();
                rc.setUserId(userId);
                rc.setRecruiterName(JSONUtil.getString(fields, "recruiterName", "Recruiter"));
                rc.setCompanyName(JSONUtil.getString(fields, "companyName", "Company"));
                rc.setCompanyDescription(JSONUtil.getString(fields, "companyDescription", ""));
                rc.setPhone(JSONUtil.getString(fields, "phone", ""));
                rc.setCountry(JSONUtil.getString(fields, "country", ""));

                boolean success = recruiterDAO.createRecruiter(rc);
                if (!success) {
                    ResponseHelper.sendError(exchange, 500, "Account created, but failed to save recruiter profile.");
                    return;
                }

                // Send welcome notification
                notificationDAO.createNotification(userId, "Welcome Recruiter",
                        "Your company recruiter account has been created! You can now post jobs and review candidates.");

                SessionManager.UserSession session = SessionManager.createSession(
                        userId, email, role, null, rc.getRecruiterId());

                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("token", session.getToken());
                resp.put("userId", userId);
                resp.put("email", email);
                resp.put("role", role);
                resp.put("name", rc.getRecruiterName());
                resp.put("recruiterId", rc.getRecruiterId());

                ResponseHelper.sendSuccess(exchange, "Recruiter registered successfully!", resp);

            } else {
                ResponseHelper.sendError(exchange, 400, "Invalid role specified: " + role);
            }

        } catch (Exception e) {
            System.err.println("[RegisterHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Registration failed due to server error.");
        }
    }
}
