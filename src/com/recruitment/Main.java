package com.recruitment;

import com.recruitment.server.Server;
import com.recruitment.util.DBConnection;

import java.io.File;

/**
 * Main Application Launcher for the Online Recruitment Management System.
 * <p>
 * Starts the embedded Core Java HTTP server and initializes application endpoints.
 * </p>
 */
public class Main {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String portProp = System.getProperty("port", System.getenv("PORT"));
        if (portProp != null && !portProp.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portProp.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Determine base paths
        String currentDir = System.getProperty("user.dir");
        File frontendDir = new File(currentDir, "frontend");
        File uploadsDir = new File(currentDir, "uploads");

        // Fallback for IDE run configs if user.dir is different
        if (!frontendDir.exists()) {
            File altFrontend = new File("frontend");
            if (altFrontend.exists()) {
                frontendDir = altFrontend;
            }
        }
        if (!uploadsDir.exists()) {
            uploadsDir = new File("uploads");
        }

        try {
            // Test MySQL Connection
            boolean dbOk = DBConnection.testConnection();

            Server server = new Server(port, frontendDir.getAbsolutePath(), uploadsDir.getAbsolutePath());
            server.start();

            // Print Startup Banner
            System.out.println("=====================================");
            System.out.println("   ONLINE RECRUITMENT SYSTEM");
            System.out.println("=====================================");
            System.out.println();
            System.out.println("Server started successfully.");
            System.out.println();
            System.out.println("Open:");
            System.out.println("http://localhost:" + port);
            System.out.println();
            if (dbOk) {
                System.out.println("[Database] Connected successfully to MySQL ('recruitment_system').");
            } else {
                System.out.println("[WARNING] Could not connect to MySQL. Ensure MySQL is running on localhost:3306");
                System.out.println("          and you have executed 'database.sql'.");
                System.out.println("          (Credentials can be configured in DBConnection.java)");
            }
            System.out.println("=====================================");
            System.out.println("Press Ctrl+C in this terminal to stop the server.");

            // Add graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                server.stop();
                System.out.println("Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("--------------------------------------------------");
            System.err.println("[FATAL] Failed to start HTTP Server on port " + port);
            System.err.println("Error: " + e.getMessage());
            System.err.println("--------------------------------------------------");
            e.printStackTrace();
        }
    }
}
