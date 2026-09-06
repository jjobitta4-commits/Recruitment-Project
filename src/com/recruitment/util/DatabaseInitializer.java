package com.recruitment.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Robustly executes database.sql against the configured MySQL instance.
 * Handles inline comments, empty lines, and statement delimiters.
 */
public class DatabaseInitializer {

    public static void initializeDatabase(String sqlFilePath) {
        System.out.println("[DatabaseInitializer] Starting database schema execution from: " + sqlFilePath);
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            int errors = 0;

            while ((line = reader.readLine()) != null) {
                // Strip inline single-line SQL comments (-- ...)
                int commentIdx = line.indexOf("--");
                if (commentIdx != -1) {
                    line = line.substring(0, commentIdx);
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("/*") || trimmed.startsWith("//")) {
                    continue;
                }

                sb.append(line).append(" ");

                // Check statement delimiter
                if (trimmed.endsWith(";")) {
                    String sql = sb.toString().trim();
                    // Remove trailing semicolon
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            count++;
                        } catch (Exception e) {
                            errors++;
                            System.err.println("[DatabaseInitializer] Error executing statement: " + e.getMessage());
                            System.err.println("Statement preview: " + (sql.length() > 100 ? sql.substring(0, 100) + "..." : sql));
                        }
                    }
                    sb.setLength(0);
                }
            }

            System.out.println("[DatabaseInitializer] Finished. Successfully executed: " + count + " statements. Errors: " + errors);

        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] Fatal initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String sqlPath = args.length > 0 ? args[0] : "database.sql";
        initializeDatabase(sqlPath);
    }
}
