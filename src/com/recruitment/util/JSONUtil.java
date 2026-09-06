package com.recruitment.util;

import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON utility for Core Java.
 * Handles parsing basic JSON objects and serializing Java objects / Maps / Lists into JSON strings.
 */
public class JSONUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Serializes any Java object, Map, List, or primitive into a valid JSON string.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Date) {
            synchronized (DATE_FORMAT) {
                return "\"" + DATE_FORMAT.format((Date) obj) + "\"";
            }
        }
        if (obj instanceof Timestamp) {
            synchronized (DATETIME_FORMAT) {
                return "\"" + DATETIME_FORMAT.format((Timestamp) obj) + "\"";
            }
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : arr) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        // POJO serialization via getter reflection
        return pojoToJson(obj);
    }

    private static String pojoToJson(Object pojo) {
        Map<String, Object> map = new LinkedHashMap<>();
        Method[] methods = pojo.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && name.length() > 3 && method.getParameterCount() == 0 && !name.equals("getClass")) {
                String propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                try {
                    Object val = method.invoke(pojo);
                    map.put(propName, val);
                } catch (Exception ignored) {}
            } else if (name.startsWith("is") && name.length() > 2 && method.getParameterCount() == 0) {
                String propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                try {
                    Object val = method.invoke(pojo);
                    map.put(propName, val);
                } catch (Exception ignored) {}
            }
        }
        return toJson(map);
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = String.format("\\u%04x", (int) c);
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Parses a JSON string representing an object into a Map<String, Object>.
     */
    public static Map<String, Object> parseObject(String json) {
        if (json == null) {
            return new HashMap<>();
        }
        json = json.trim();
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1).trim();
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            return new HashMap<>();
        }
        json = json.substring(start, end + 1);
        Tokenizer tokenizer = new Tokenizer(json);
        Object res = tokenizer.parseValue();
        if (res instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) res;
            return map;
        }
        return new HashMap<>();
    }

    public static String getString(Map<String, Object> map, String key, String defaultVal) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) return defaultVal;
        return String.valueOf(map.get(key));
    }

    public static int getInt(Map<String, Object> map, String key, int defaultVal) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) return defaultVal;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static Integer getIntegerOrNull(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) return null;
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            String s = String.valueOf(val).trim();
            if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date getDateOrNull(Map<String, Object> map, String key) {
        String str = getString(map, key, null);
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return Date.valueOf(str.trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================================
    // Internal JSON recursive descent parser
    // ==========================================
    private static class Tokenizer {
        private final String src;
        private int pos = 0;

        public Tokenizer(String src) {
            this.src = src;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        public Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') {
                return parseObjectInternal();
            } else if (c == '[') {
                return parseArrayInternal();
            } else if (c == '"' || c == '\'') {
                return parseString();
            } else if (c == 't' || c == 'f') {
                return parseBoolean();
            } else if (c == 'n') {
                return parseNull();
            } else if (c == '-' || Character.isDigit(c)) {
                return parseNumber();
            }
            return null;
        }

        private Map<String, Object> parseObjectInternal() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++; // empty map
                return map;
            }

            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') {
                    pos++; // skip ':'
                }
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                } else {
                    pos++;
                }
            }
            return map;
        }

        private List<Object> parseArrayInternal() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                } else {
                    pos++;
                }
            }
            return list;
        }

        private String parseString() {
            char quote = src.charAt(pos);
            if (quote != '"' && quote != '\'') return "";
            pos++; // skip open quote
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == quote) {
                    pos++; // skip end quote
                    break;
                } else if (c == '\\' && pos + 1 < src.length()) {
                    pos++;
                    char esc = src.charAt(pos);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 < src.length()) {
                                String hex = src.substring(pos + 1, pos + 5);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default: sb.append(esc); break;
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", pos)) {
                pos += 4;
                return true;
            } else if (src.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            return false;
        }

        private Object parseNull() {
            if (src.startsWith("null", pos)) {
                pos += 4;
            }
            return null;
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < src.length() && src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.' || src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
                pos++;
            }
            String numStr = src.substring(start, pos);
            if (numStr.contains(".")) {
                try { return Double.parseDouble(numStr); } catch (Exception ignored) {}
            }
            try { return Long.parseLong(numStr); } catch (Exception ignored) {}
            return 0;
        }
    }
}
