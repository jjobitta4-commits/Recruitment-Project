package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.model.Candidate;
import com.recruitment.model.SkillGapReport;
import com.recruitment.service.SkillGapService;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * HTTP Handler for Core Innovation 2: Skill Gap Analyzer & Learning Roadmap Generator.
 * Provides deep deficiency auditing, prerequisite verification, and learning roadmaps.
 */
public class SkillGapHandler implements HttpHandler {

    private final SkillGapService skillGapService = new SkillGapService();
    private final CandidateDAO candidateDAO = new CandidateDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        if (!"GET".equals(method)) {
            ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> query = ResponseHelper.parseQueryParams(exchange.getRequestURI().getRawQuery());
        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);

        try {
            int jobId = 0;
            if (query.containsKey("jobId")) {
                try { jobId = Integer.parseInt(query.get("jobId")); } catch (Exception ignored) {}
            }
            if (jobId <= 0) {
                ResponseHelper.sendError(exchange, 400, "Missing or invalid 'jobId' query parameter.");
                return;
            }

            int candidateId = 0;
            if (query.containsKey("candidateId")) {
                try { candidateId = Integer.parseInt(query.get("candidateId")); } catch (Exception ignored) {}
            } else if (session != null && session.isCandidate()) {
                Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
                if (c != null) candidateId = c.getCandidateId();
            }

            if (candidateId <= 0) {
                ResponseHelper.sendError(exchange, 400, "Candidate identity required to perform skill gap analysis.");
                return;
            }

            SkillGapReport report = skillGapService.analyzeSkillGap(candidateId, jobId);
            ResponseHelper.sendSuccess(exchange, "Skill gap analysis completed", report);

        } catch (Exception e) {
            System.err.println("[SkillGapHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error during skill gap analysis: " + e.getMessage());
        }
    }
}
