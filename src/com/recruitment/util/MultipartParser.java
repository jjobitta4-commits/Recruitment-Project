package com.recruitment.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pure Core Java parser for multipart/form-data HTTP requests.
 * Allows uploading PDF resumes and extracting form fields without external libraries.
 */
public class MultipartParser {

    public static class FileItem {
        private final String fieldName;
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public FileItem(String fieldName, String fileName, String contentType, byte[] content) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFieldName() { return fieldName; }
        public String getFileName() { return fileName; }
        public String getContentType() { return contentType; }
        public byte[] getContent() { return content; }
        public long getSize() { return content != null ? content.length : 0; }
    }

    public static class MultipartResult {
        private final Map<String, String> fields = new HashMap<>();
        private final List<FileItem> files = new ArrayList<>();

        public void addField(String key, String value) { fields.put(key, value); }
        public void addFile(FileItem file) { files.add(file); }

        public String getField(String key) { return fields.get(key); }
        public Map<String, String> getFields() { return fields; }
        public List<FileItem> getFiles() { return files; }
        public FileItem getFirstFile() { return files.isEmpty() ? null : files.get(0); }
    }

    /**
     * Parses a multipart/form-data byte payload given the Content-Type header.
     */
    public static MultipartResult parse(String contentTypeHeader, byte[] bodyBytes) throws IOException {
        MultipartResult result = new MultipartResult();
        if (contentTypeHeader == null || !contentTypeHeader.toLowerCase().contains("multipart/form-data")) {
            return result;
        }

        // Extract boundary parameter
        String boundary = null;
        for (String param : contentTypeHeader.split(";")) {
            param = param.trim();
            if (param.toLowerCase().startsWith("boundary=")) {
                boundary = param.substring("boundary=".length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                break;
            }
        }

        if (boundary == null || boundary.isEmpty()) {
            throw new IOException("Missing boundary in Content-Type header");
        }

        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        List<int[]> parts = findPartIndices(bodyBytes, boundaryBytes);

        for (int[] partRange : parts) {
            int start = partRange[0];
            int end = partRange[1];
            if (start >= end) continue;

            // Find separator between headers and content: \r\n\r\n
            int headerEnd = indexOf(bodyBytes, new byte[]{'\r', '\n', '\r', '\n'}, start, end);
            if (headerEnd == -1) continue;

            String headerText = new String(bodyBytes, start, headerEnd - start, StandardCharsets.UTF_8);
            int bodyStart = headerEnd + 4;
            int bodyEnd = end;

            // Trim trailing \r\n if present before boundary
            if (bodyEnd >= bodyStart + 2 && bodyBytes[bodyEnd - 2] == '\r' && bodyBytes[bodyEnd - 1] == '\n') {
                bodyEnd -= 2;
            }

            Map<String, String> headers = parseHeaders(headerText);
            String disp = headers.get("content-disposition");
            if (disp == null) continue;

            String fieldName = extractHeaderParam(disp, "name");
            String fileName = extractHeaderParam(disp, "filename");
            String partContentType = headers.getOrDefault("content-type", "text/plain");

            if (fileName != null) {
                // File part
                int length = bodyEnd - bodyStart;
                byte[] fileBytes = new byte[Math.max(0, length)];
                if (length > 0) {
                    System.arraycopy(bodyBytes, bodyStart, fileBytes, 0, length);
                }
                result.addFile(new FileItem(fieldName, fileName, partContentType, fileBytes));
            } else if (fieldName != null) {
                // Text field
                int length = bodyEnd - bodyStart;
                String value = (length > 0) ? new String(bodyBytes, bodyStart, length, StandardCharsets.UTF_8) : "";
                result.addField(fieldName, value);
            }
        }

        return result;
    }

    private static Map<String, String> parseHeaders(String headerText) {
        Map<String, String> map = new HashMap<>();
        String[] lines = headerText.split("\r\n");
        for (String line : lines) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                String key = line.substring(0, idx).trim().toLowerCase();
                String val = line.substring(idx + 1).trim();
                map.put(key, val);
            }
        }
        return map;
    }

    private static String extractHeaderParam(String header, String paramName) {
        String[] parts = header.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.toLowerCase().startsWith(paramName.toLowerCase() + "=")) {
                String val = part.substring(paramName.length() + 1).trim();
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1);
                }
                return val;
            }
        }
        return null;
    }

    private static List<int[]> findPartIndices(byte[] data, byte[] boundary) {
        List<int[]> ranges = new ArrayList<>();
        int lastPos = -1;
        int pos = 0;

        while (pos <= data.length - boundary.length) {
            if (match(data, boundary, pos)) {
                if (lastPos != -1) {
                    // Range between last boundary and this boundary
                    int partStart = lastPos + boundary.length;
                    // skip leading \r\n after boundary
                    if (partStart + 1 < data.length && data[partStart] == '\r' && data[partStart + 1] == '\n') {
                        partStart += 2;
                    }
                    ranges.add(new int[]{partStart, pos});
                }
                lastPos = pos;
                pos += boundary.length;
            } else {
                pos++;
            }
        }
        return ranges;
    }

    private static boolean match(byte[] data, byte[] target, int offset) {
        if (offset + target.length > data.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (data[offset + i] != target[i]) return false;
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] target, int start, int end) {
        for (int i = start; i <= end - target.length; i++) {
            if (match(data, target, i)) return i;
        }
        return -1;
    }
}
