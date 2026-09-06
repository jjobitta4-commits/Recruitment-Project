package com.recruitment.server;

import com.recruitment.util.DBConnection;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main HTTP Server orchestrator built directly upon com.sun.net.httpserver.HttpServer.
 * Provides thread-pooled non-blocking connection dispatch, dynamic port fallback (8080-8085),
 * route registration, and graceful shutdown lifecycle management.
 */
public class MainServer {

    private final int port;
    private final String frontendDir;
    private final String uploadsDir;
    private HttpServer httpServer;

    public MainServer(int port, String frontendDir, String uploadsDir) {
        this.port = port;
        this.frontendDir = frontendDir;
        this.uploadsDir = uploadsDir;
    }

    /**
     * Binds and starts the HTTP server on the configured port.
     */
    public void start() throws IOException {
        // Ensure upload storage directories exist
        File resumesDir = new File(uploadsDir, "resumes");
        if (!resumesDir.exists()) {
            resumesDir.mkdirs();
        }

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Core Foundation & Health check
        httpServer.createContext("/api/health", new HealthHandler());

        // Authentication & Session Endpoints
        httpServer.createContext("/api/login", new LoginHandler());
        httpServer.createContext("/api/logout", new LoginHandler());
        httpServer.createContext("/api/session", new LoginHandler());
        httpServer.createContext("/api/register", new RegisterHandler(uploadsDir));
        httpServer.createContext("/api/verify-email", new EmailVerificationHandler());

        // Module Endpoints
        httpServer.createContext("/api/jobs", new JobHandler());
        httpServer.createContext("/api/applications", new ApplicationHandler(uploadsDir));
        httpServer.createContext("/api/recruiter", new RecruiterHandler());
        httpServer.createContext("/api/applicant", new ApplicantHandler(uploadsDir));
        httpServer.createContext("/api/candidate", new CandidateHandler(uploadsDir));
        httpServer.createContext("/api/interviews", new InterviewHandler());
        httpServer.createContext("/api/notifications", new NotificationHandler());
        httpServer.createContext("/api/admin", new AdminHandler());
        httpServer.createContext("/api/companies", new CompanyHandler());
        httpServer.createContext("/api/match", new MatchHandler());
        httpServer.createContext("/api/skill-gap", new SkillGapHandler());
        httpServer.createContext("/api/assessments", new AssessmentHandler());
        httpServer.createContext("/api/career-paths", new CareerPathHandler());
        httpServer.createContext("/api/career-path", new CareerPathHandler());

        // Static Assets & Routing
        httpServer.createContext("/", new StaticFileHandler(frontendDir, uploadsDir));

        // Thread pool executor for high concurrent throughput
        httpServer.setExecutor(Executors.newFixedThreadPool(16));
        httpServer.start();
    }

    /**
     * Gracefully stops the HTTP server.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }

    public int getPort() {
        return port;
    }

    /**
     * Standalone main runner with automated port conflict resolution.
     */
    public static void main(String[] args) {
        int initialPort = 8080;
        String portProp = System.getProperty("port", System.getenv("PORT"));
        if (portProp != null && !portProp.trim().isEmpty()) {
            try {
                initialPort = Integer.parseInt(portProp.trim());
            } catch (NumberFormatException ignored) {}
        }

        String currentDir = System.getProperty("user.dir");
        File frontendDir = new File(currentDir, "frontend");
        File uploadsDir = new File(currentDir, "uploads");

        if (!frontendDir.exists()) {
            File altFrontend = new File("frontend");
            if (altFrontend.exists()) frontendDir = altFrontend;
        }
        if (!uploadsDir.exists()) {
            uploadsDir = new File("uploads");
        }

        // Test Database Connectivity
        boolean dbConnected = DBConnection.testConnection();
        if (dbConnected) {
            com.recruitment.util.DBMigration.runMigrations();
        }

        MainServer server = null;
        int activePort = initialPort;
        for (int offset = 0; offset < 6; offset++) {
            try {
                activePort = initialPort + offset;
                server = new MainServer(activePort, frontendDir.getAbsolutePath(), uploadsDir.getAbsolutePath());
                server.start();
                break;
            } catch (java.net.BindException be) {
                System.out.println("[MainServer] Port " + activePort + " in use. Trying port " + (activePort + 1) + "...");
            } catch (IOException e) {
                System.err.println("[MainServer] Failed to bind to port " + activePort + ": " + e.getMessage());
                break;
            }
        }

        if (server == null) {
            System.err.println("[FATAL] Could not bind to any port between " + initialPort + " and " + activePort);
            return;
        }

        System.out.println("===============================================================");
        System.out.println("   SMART RECRUITMENT AND CANDIDATE SKILL MATCHING SYSTEM");
        System.out.println("===============================================================");
        System.out.println(" Server successfully running at: http://localhost:" + activePort);
        System.out.println(" Health check endpoint:          http://localhost:" + activePort + "/api/health");
        System.out.println(" Database connection status:     " + (dbConnected ? "CONNECTED [recruitment_system]" : "DISCONNECTED"));
        System.out.println(" Static frontend directory:      " + frontendDir.getAbsolutePath());
        System.out.println(" Uploads repository:             " + uploadsDir.getAbsolutePath());
        System.out.println("===============================================================");
        System.out.println("Press Ctrl+C to terminate the server.");

        final MainServer finalServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[MainServer] Gracefully shutting down HTTP server...");
            finalServer.stop();
            System.out.println("[MainServer] Server stopped.");
        }));
    }
}
