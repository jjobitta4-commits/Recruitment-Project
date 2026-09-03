package com.recruitment.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper utility for reading HTTP requests and sending JSON HTTP responses.
 */
public class ResponseHelper {

    /**
     * Reads the complete UTF-8 request body as a String.
     */
    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Reads the request body as raw bytes (for file uploads).
     */
    public static byte[] readBodyBytes(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    /**
     * Parses URL query parameters into a Map.
     */
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(key, value);
                } else if (!pair.isEmpty()) {
                    map.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (UnsupportedEncodingException ignored) {}
        }
        return map;
    }

    /**
     * Sends a successful JSON response with HTTP 200.
     */
    public static void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        if (data != null) {
            resp.put("data", data);
        }
        sendJson(exchange, 200, resp);
    }

    /**
     * Sends a successful JSON response without extra data payload.
     */
    public static void sendSuccess(HttpExchange exchange, String message) throws IOException {
        sendSuccess(exchange, message, null);
    }

    /**
     * Sends an error JSON response with custom HTTP status code.
     */
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        sendJson(exchange, statusCode, resp);
    }

    /**
     * Sends raw JSON string with specified HTTP status.
     */
    public static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = (data instanceof String) ? (String) data : JSONUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-Token");

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        }
    }

    /**
     * Handles CORS preflight OPTIONS requests.
     */
    public static boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-Token");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }
}
