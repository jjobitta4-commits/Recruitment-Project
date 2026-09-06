package com.recruitment.server;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.model.Candidate;
import com.recruitment.model.Job;
import com.recruitment.model.MatchResult;
import com.recruitment.service.JobMatchingService;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Core Innovation 1: Smart Job Matching Engine.
 * Provides real-time 5-factor weighted compatibility evaluation and job ranking endpoints.
 */
public class MatchHandler implements HttpHandler {

    private final JobMatchingService matchingService = new JobMatchingService();
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
            String action = query.get("action");

            if ("ranked-jobs".equalsIgnoreCase(action)) {
                // Return all active vacancies auto-ranked for the candidate
                if (session == null || !session.isCandidate()) {
                    ResponseHelper.sendError(exchange, 401, "Candidate authentication required for personalized job ranking.");
                    return;
                }
                Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
                if (c == null) {
                    ResponseHelper.sendError(exchange, 404, "Candidate profile not found.");
                    return;
                }
                List<Job> ranked = matchingService.getRankedJobsForCandidate(c.getCandidateId());
                ResponseHelper.sendSuccess(exchange, "Ranked jobs for candidate", ranked);
                return;
            }

            // Specific Job Matching
            int jobId = 0;
            if (query.containsKey("jobId")) {
                try { jobId = Integer.parseInt(query.get("jobId")); } catch (Exception ignored) {}
            }
            if (jobId <= 0) {
                ResponseHelper.sendError(exchange, 400, "Missing or invalid 'jobId' query parameter.");
                return;
            }

            // Determine candidateId
            int candidateId = 0;
            if (query.containsKey("candidateId")) {
                try { candidateId = Integer.parseInt(query.get("candidateId")); } catch (Exception ignored) {}
            } else if (session != null && session.isCandidate()) {
                Candidate c = candidateDAO.getCandidateByUserId(session.getUserId());
                if (c != null) candidateId = c.getCandidateId();
            }

            if (candidateId <= 0) {
                ResponseHelper.sendError(exchange, 400, "Candidate identity required to compute match score.");
                return;
            }

            MatchResult match = matchingService.calculateMatch(candidateId, jobId);
            ResponseHelper.sendSuccess(exchange, "Match evaluation completed", match);

        } catch (Exception e) {
            System.err.println("[MatchHandler] Error: " + e.getMessage());
            e.printStackTrace();
            ResponseHelper.sendError(exchange, 500, "Internal error during match computation: " + e.getMessage());
        }
    }
}
