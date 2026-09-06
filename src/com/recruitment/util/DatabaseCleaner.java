package com.recruitment.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility to cleanly purge default recruiters, candidates, jobs, and applications
 * while strictly preserving the Admin account (admin@recruithub.com).
 */
public class DatabaseCleaner {

    public static void cleanDatabase() {
        System.out.println("[DatabaseCleaner] Starting database purge...");
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[DatabaseCleaner] Failed to obtain database connection.");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");

                // Purge candidate data
                stmt.executeUpdate("DELETE FROM assessment_answers;");
                stmt.executeUpdate("DELETE FROM assessment_attempts;");
                stmt.executeUpdate("DELETE FROM candidate_skills;");
                stmt.executeUpdate("DELETE FROM education;");
                stmt.executeUpdate("DELETE FROM experience;");
                stmt.executeUpdate("DELETE FROM projects;");
                stmt.executeUpdate("DELETE FROM resumes;");
                stmt.executeUpdate("DELETE FROM certifications;");
                stmt.executeUpdate("DELETE FROM applications;");
                stmt.executeUpdate("DELETE FROM interviews;");
                stmt.executeUpdate("DELETE FROM candidates;");

                // Purge recruiter data & jobs
                stmt.executeUpdate("DELETE FROM job_skills;");
                stmt.executeUpdate("DELETE FROM jobs;");
                stmt.executeUpdate("DELETE FROM recruiters;");

                // Unlink job_id from assessments so assessments & questions remain available
                stmt.executeUpdate("UPDATE assessments SET job_id = NULL;");

                // Clean notifications for non-admin
                stmt.executeUpdate("DELETE FROM notifications WHERE user_id NOT IN (SELECT user_id FROM (SELECT user_id FROM users WHERE email = 'admin@recruithub.com') AS tmp);");

                // Clean email verifications except admin
                stmt.executeUpdate("DELETE FROM email_verifications WHERE email != 'admin@recruithub.com';");

                // Delete all users except admin@recruithub.com
                int deletedUsers = stmt.executeUpdate("DELETE FROM users WHERE email != 'admin@recruithub.com';");
                System.out.println("[DatabaseCleaner] Deleted non-admin users: " + deletedUsers);

                // Ensure admin exists and is active with valid password
                // admin123 hashed with salt kX8f9w1zL2mP4qRt is b1f6305a4687de6478677f98b17b63f5385628b0303b7a5e55543c7b274ab359
                boolean adminExists = false;
                try (ResultSet rs = stmt.executeQuery("SELECT user_id FROM users WHERE email = 'admin@recruithub.com'")) {
                    if (rs.next()) {
                        adminExists = true;
                    }
                }

                if (adminExists) {
                    stmt.executeUpdate(
                        "UPDATE users SET role = 'admin', status = 'active', is_verified = TRUE, " +
                        "password_hash = '2069528d0465ab0a75a00a76d1139b34ed357dee6f0fe1b748bd2223ec37e18a', " +
                        "salt = 'kX8f9w1zL2mP4qRt' WHERE email = 'admin@recruithub.com';"
                    );
                    System.out.println("[DatabaseCleaner] Admin account (admin@recruithub.com) verified and secured.");
                } else {
                    stmt.executeUpdate(
                        "INSERT INTO users (user_id, email, password_hash, salt, role, status, is_verified) VALUES " +
                        "(1, 'admin@recruithub.com', '2069528d0465ab0a75a00a76d1139b34ed357dee6f0fe1b748bd2223ec37e18a', 'kX8f9w1zL2mP4qRt', 'admin', 'active', TRUE);"
                    );
                    System.out.println("[DatabaseCleaner] Inserted default admin account (admin@recruithub.com).");
                }

                stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            }

            // Print table counts
            try (Statement stmt = conn.createStatement()) {
                String[] tables = {"users", "candidates", "recruiters", "jobs", "applications", "interviews", "assessment_attempts"};
                for (String t : tables) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + t)) {
                        if (rs.next()) {
                            System.out.println("[DatabaseCleaner] Table '" + t + "' row count: " + rs.getInt(1));
                        }
                    }
                }
            }

            System.out.println("[DatabaseCleaner] Database purge successfully completed.");

        } catch (Exception e) {
            System.err.println("[DatabaseCleaner] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        cleanDatabase();
    }
}
