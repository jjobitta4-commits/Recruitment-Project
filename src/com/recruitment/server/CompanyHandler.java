package com.recruitment.server;

import com.recruitment.model.Company;
import com.recruitment.service.CompanyService;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for Enterprise / Company directory.
 */
public class CompanyHandler implements HttpHandler {

    private final CompanyService companyService = new CompanyService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        try {
            if ("GET".equals(method)) {
                List<Company> list = companyService.getApprovedCompanies();
                ResponseHelper.sendSuccess(exchange, "Approved companies list", list);
            } else if ("POST".equals(method)) {
                String body = ResponseHelper.readBody(exchange);
                Map<String, Object> data = JSONUtil.parseObject(body);
                String name = JSONUtil.getString(data, "name", "").trim();

                if (name.isEmpty()) {
                    ResponseHelper.sendError(exchange, 400, "Company name is required.");
                    return;
                }

                Company c = new Company();
                c.setName(name);
                c.setDescription(JSONUtil.getString(data, "description", ""));
                c.setIndustry(JSONUtil.getString(data, "industry", "Technology"));
                c.setWebsite(JSONUtil.getString(data, "website", ""));
                c.setLocation(JSONUtil.getString(data, "location", ""));
                c.setApprovalStatus("approved");

                boolean ok = companyService.registerCompany(c);
                if (ok) {
                    ResponseHelper.sendSuccess(exchange, "Company registered successfully", c);
                } else {
                    ResponseHelper.sendError(exchange, 500, "Failed to register company.");
                }
            } else {
                ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("[CompanyHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }
}
