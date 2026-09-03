package com.recruitment.server;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP Server using Java's built-in com.sun.net.httpserver.HttpServer.
 */
public class Server {

    private final int port;
    private final String frontendDir;
    private final String uploadsDir;
    private HttpServer httpServer;

    public Server(int port, String frontendDir, String uploadsDir) {
        this.port = port;
        this.frontendDir = frontendDir;
        this.uploadsDir = uploadsDir;
    }

    /**
     * Initializes contexts and starts the HTTP server.
     */
    public void start() throws IOException {
        // Ensure upload directories exist
        File resumesDir = new File(uploadsDir, "resumes");
        if (!resumesDir.exists()) {
            resumesDir.mkdirs();
        }

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Register API REST endpoints
        httpServer.createContext("/api/login", new LoginHandler());
        httpServer.createContext("/api/logout", new LoginHandler());
        httpServer.createContext("/api/session", new LoginHandler());
        httpServer.createContext("/api/register", new RegisterHandler(uploadsDir));
        httpServer.createContext("/api/jobs", new JobHandler());
        httpServer.createContext("/api/applications", new ApplicationHandler(uploadsDir));
        httpServer.createContext("/api/recruiter", new RecruiterHandler());
        httpServer.createContext("/api/applicant", new ApplicantHandler(uploadsDir));
        httpServer.createContext("/api/interviews", new InterviewHandler());
        httpServer.createContext("/api/notifications", new NotificationHandler());

        // Register static file handler for frontend and uploaded files
        httpServer.createContext("/", new StaticFileHandler(frontendDir, uploadsDir));

        // Use thread pool for concurrent request handling
        httpServer.setExecutor(Executors.newFixedThreadPool(16));

        httpServer.start();
    }

    /**
     * Stops the HTTP server gracefully.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }

    public int getPort() {
        return port;
    }
}
