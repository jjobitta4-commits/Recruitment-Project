package com.recruitment.server;

import com.recruitment.util.DBConnection;
import com.recruitment.util.ResponseHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check handler returning JSON status of the server and MySQL database.
 * Responds to GET /api/health.
 */
public class HealthHandler implements HttpHandler {

    private static final long SERVER_START_TIME = System.currentTimeMillis();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        boolean dbConnected = false;
        int tableCount = 0;
        String dbError = null;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES;")) {
            while (rs.next()) {
                tableCount++;
            }
            dbConnected = true;
        } catch (Exception e) {
            dbConnected = false;
            dbError = e.getMessage();
        }

        long uptimeSeconds = (System.currentTimeMillis() - SERVER_START_TIME) / 1000;
        String nowFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        response.put("success", true);
        response.put("status", dbConnected ? "UP" : "DEGRADED");
        response.put("system", "Smart Recruitment and Candidate Skill Matching System");
        response.put("version", "2.0.0");
        response.put("timestamp", nowFormatted);
        response.put("uptimeSeconds", uptimeSeconds);

        Map<String, Object> dbInfo = new LinkedHashMap<>();
        dbInfo.put("status", dbConnected ? "CONNECTED" : "DISCONNECTED");
        dbInfo.put("database", "recruitment_system");
        dbInfo.put("tablesCount", tableCount);
        if (dbError != null) {
            dbInfo.put("error", dbError);
        }
        response.put("database", dbInfo);

        Map<String, Object> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("javaVersion", System.getProperty("java.version"));
        runtimeInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        runtimeInfo.put("freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024));
        runtimeInfo.put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024));
        response.put("runtime", runtimeInfo);

        ResponseHelper.sendJson(exchange, dbConnected ? 200 : 503, response);
    }
}
