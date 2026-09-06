package com.recruitment.server;

import com.recruitment.util.ResponseHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Static file server for serving Frontend HTML, CSS, JavaScript, and Uploaded Files.
 * Includes path traversal protection and automatic MIME type resolution.
 */
public class StaticFileHandler implements HttpHandler {

    private final String frontendRoot;
    private final String uploadsRoot;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
    }

    public StaticFileHandler(String frontendRoot, String uploadsRoot) {
        this.frontendRoot = frontendRoot;
        this.uploadsRoot = uploadsRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Prevent directory traversal attacks
        if (path.contains("..") || path.contains("./") || path.contains("//")) {
            ResponseHelper.sendError(exchange, 403, "Access Forbidden");
            return;
        }

        File targetFile;
        File baseDir;

        if (path.startsWith("/uploads/")) {
            String relPath = path.substring("/uploads/".length());
            baseDir = new File(uploadsRoot).getCanonicalFile();
            targetFile = new File(baseDir, relPath).getCanonicalFile();
        } else {
            if (path.startsWith("/frontend/")) {
                path = path.substring("/frontend".length());
            }
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            } else if (path.equals("/candidate") || path.equals("/candidate/")) {
                path = "/candidate/dashboard.html";
            } else if (path.equals("/applicant") || path.equals("/applicant/")) {
                path = "/applicant/dashboard.html";
            } else if (path.equals("/recruiter") || path.equals("/recruiter/")) {
                path = "/recruiter/dashboard.html";
            } else if (path.equals("/admin") || path.equals("/admin/")) {
                path = "/admin/dashboard.html";
            }

            String safePath = path.startsWith("/") ? path.substring(1) : path;
            baseDir = new File(frontendRoot).getCanonicalFile();
            targetFile = new File(baseDir, safePath).getCanonicalFile();
        }

        // Verify target remains within allowed directory boundary
        if (!targetFile.getPath().startsWith(baseDir.getPath())) {
            ResponseHelper.sendError(exchange, 403, "Access Denied");
            return;
        }

        if (!targetFile.exists() || targetFile.isDirectory()) {
            // Check if adding .html helps (e.g. /login -> /login.html)
            String safePath = path.startsWith("/") ? path.substring(1) : path;
            File htmlVariant = new File(baseDir, safePath + ".html").getCanonicalFile();
            if (htmlVariant.exists() && !htmlVariant.isDirectory() && htmlVariant.getPath().startsWith(baseDir.getPath())) {
                targetFile = htmlVariant;
            } else {
                ResponseHelper.sendError(exchange, 404, "Resource Not Found: " + path);
                return;
            }
        }

        // Determine content type
        String fileName = targetFile.getName();
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < fileName.length() - 1) {
            ext = fileName.substring(dotIdx + 1).toLowerCase();
        }
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        // Send headers and file content
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
        if ("pdf".equals(ext)) {
            exchange.getResponseHeaders().set("Content-Disposition", "inline; filename=\"" + fileName + "\"");
        }
        exchange.sendResponseHeaders(200, targetFile.length());

        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (FileInputStream fis = new FileInputStream(targetFile);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        } else {
            exchange.getResponseBody().close();
        }
    }
}
