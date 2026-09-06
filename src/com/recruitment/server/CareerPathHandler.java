package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.CareerPathDAO;
import com.recruitment.model.*;
import com.recruitment.service.CareerPathService;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise HTTP Handler for Career Path Recommendation Engine (Core Innovation 5).
 * Registered in MainServer at /api/career-paths.
 */
public class CareerPathHandler implements HttpHandler {

    private final CareerPathService careerPathService = new CareerPathService();
    private final CareerPathDAO careerPathDAO = new CareerPathDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();

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
                    handleGet(exchange, path);
                    break;

                case "POST":
                    handlePost(exchange, path);
                    break;

                default:
                    ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            ResponseHelper.sendError(exchange, 400, e.getMessage());
        } catch (SecurityException e) {
            ResponseHelper.sendError(exchange, 403, e.getMessage());
        } catch (Exception e) {
            System.err.println("[CareerPathHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error in Career Path Engine: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);

        int candidateId = -1;
        if (session != null && "candidate".equalsIgnoreCase(session.getRole())) {
            candidateId = resolveCandidateId(session);
        } else if (query.containsKey("candidateId")) {
            try {
                candidateId = Integer.parseInt(query.get("candidateId"));
            } catch (NumberFormatException ignored) {}
        }

        // Check if single path requested: /api/career-paths/{id} or ?pathId={id}
        int requestedPathId = -1;
        if (path.matches(".*/api/career-paths/\\d+")) {
            try {
                requestedPathId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
            } catch (NumberFormatException ignored) {}
        } else if (query.containsKey("pathId")) {
            try {
                requestedPathId = Integer.parseInt(query.get("pathId"));
            } catch (NumberFormatException ignored) {}
        }

        if (requestedPathId > 0) {
            if (candidateId > 0) {
                CareerRecommendation rec = careerPathService.getRecommendation(candidateId, requestedPathId);
                if (rec != null) {
                    ResponseHelper.sendSuccess(exchange, "Personalized career path evaluation retrieved", rec);
                    return;
                }
            }
            CareerPath cp = careerPathDAO.getCareerPathById(requestedPathId);
            if (cp == null) {
                ResponseHelper.sendError(exchange, 404, "Career path not found with ID: " + requestedPathId);
                return;
            }
            ResponseHelper.sendSuccess(exchange, "Career path details retrieved", cp);
            return;
        }

        // If candidate logged in, return complete personalized recommendations list
        if (candidateId > 0) {
            List<CareerRecommendation> recs = careerPathService.getRecommendationsForCandidate(candidateId);
            Map<String, Object> data = new HashMap<>();
            data.put("candidateId", candidateId);
            data.put("recommendations", recs);
            if (!recs.isEmpty()) {
                data.put("topMatch", recs.get(0));
            }
            ResponseHelper.sendSuccess(exchange, "Career recommendations synthesized successfully", data);
            return;
        }

        // If unauthenticated or recruiter/admin, return all career paths
        List<CareerPath> allPaths = careerPathDAO.getAllCareerPaths();
        ResponseHelper.sendSuccess(exchange, "All industry career tracks retrieved", allPaths);
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        if (!"admin".equalsIgnoreCase(session.getRole()) && !"recruiter".equalsIgnoreCase(session.getRole())) {
            ResponseHelper.sendError(exchange, 403, "Only Administrators and Recruiters can create career paths.");
            return;
        }

        String body = ResponseHelper.readBody(exchange);
        Map<String, Object> json = JSONUtil.parseObject(body);

        if (path.endsWith("/milestones")) {
            // Add milestone to path
            int pathId = JSONUtil.getInt(json, "pathId", 0);
            int levelOrder = JSONUtil.getInt(json, "levelOrder", 1);
            String levelName = JSONUtil.getString(json, "levelName", "");
            String expRange = JSONUtil.getString(json, "experienceYearsRange", "");
            String salaryRange = JSONUtil.getString(json, "salaryRange", "");
            String skillsRequired = JSONUtil.getString(json, "skillsRequired", "");
            String description = JSONUtil.getString(json, "milestoneDescription", "");
            String action = JSONUtil.getString(json, "recommendedAction", "");

            if (pathId <= 0 || levelName.isEmpty() || skillsRequired.isEmpty()) {
                ResponseHelper.sendError(exchange, 400, "Missing required fields: pathId, levelName, skillsRequired");
                return;
            }

            CareerMilestone cm = new CareerMilestone(0, pathId, levelOrder, levelName, expRange, salaryRange,
                    skillsRequired, description, action);
            boolean ok = careerPathDAO.createMilestone(cm);
            if (ok) {
                ResponseHelper.sendSuccess(exchange, "Career milestone created successfully", cm);
            } else {
                ResponseHelper.sendError(exchange, 500, "Failed to create career milestone.");
            }
        } else {
            // Create new career path
            String title = JSONUtil.getString(json, "title", "");
            String category = JSONUtil.getString(json, "category", "General");
            String description = JSONUtil.getString(json, "description", "");
            String skills = JSONUtil.getString(json, "requiredCoreSkills", "");
            int minExp = JSONUtil.getInt(json, "minStartingExperienceYears", 0);
            String salary = JSONUtil.getString(json, "averageMarketSalary", "$100,000+");

            if (title.isEmpty() || skills.isEmpty()) {
                ResponseHelper.sendError(exchange, 400, "Missing required fields: title, requiredCoreSkills");
                return;
            }

            CareerPath cp = new CareerPath(0, title, category, description, skills, minExp, salary, null);
            boolean ok = careerPathDAO.createCareerPath(cp);
            if (ok) {
                ResponseHelper.sendSuccess(exchange, "Career path track created successfully", cp);
            } else {
                ResponseHelper.sendError(exchange, 500, "Failed to create career path track.");
            }
        }
    }

    private int resolveCandidateId(SessionManager.UserSession session) {
        if (session == null) return -1;
        Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
        return c != null ? c.getCandidateId() : -1;
    }
}
