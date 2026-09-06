package com.recruitment.util;

import java.util.*;

public class SkillMatcher {

    public static Map<String, Object> calculateMatch(
            String candidateSkillsStr,
            int candidateExpYears,
            String requiredSkillsStr,
            String requiredExpStr) {

        Set<String> candTokens = extractSkillTokens(candidateSkillsStr);
        Set<String> reqTokens = extractSkillTokens(requiredSkillsStr);

        if (reqTokens.isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("score", 100);
            fallback.put("level", "EXCELLENT");
            fallback.put("matchedSkills", new ArrayList<>(candTokens));
            fallback.put("missingSkills", Collections.emptyList());
            return fallback;
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String req : reqTokens) {
            boolean found = false;
            for (String cand : candTokens) {
                if (isMatch(cand, req)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                matched.add(capitalize(req));
            } else {
                missing.add(capitalize(req));
            }
        }

        double skillRatio = (double) matched.size() / reqTokens.size();
        double baseScore = skillRatio * 85.0;

        int reqExpMin = parseMinExperience(requiredExpStr);
        double expBonus = 0;
        if (reqExpMin <= 0) {
            expBonus = 15.0;
        } else if (candidateExpYears >= reqExpMin) {
            expBonus = 15.0;
        } else if (candidateExpYears > 0) {
            expBonus = ((double) candidateExpYears / reqExpMin) * 12.0;
        }

        int finalScore = (int) Math.round(Math.min(100.0, Math.max(0.0, baseScore + expBonus)));

        String level;
        if (finalScore >= 85) {
            level = "EXCELLENT";
        } else if (finalScore >= 65) {
            level = "GOOD";
        } else if (finalScore >= 45) {
            level = "MODERATE";
        } else {
            level = "LOW";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", finalScore);
        result.put("level", level);
        result.put("matchedSkills", matched);
        result.put("missingSkills", missing);
        result.put("matchRatio", String.format("%d/%d Skills", matched.size(), reqTokens.size()));
        return result;
    }

    private static Set<String> extractSkillTokens(String raw) {
        Set<String> set = new LinkedHashSet<>();
        if (raw == null || raw.trim().isEmpty()) return set;

        String[] parts = raw.split("[,;/|\\n\\r]+");
        for (String p : parts) {
            String clean = p.trim().toLowerCase();
            clean = clean.replaceAll("^(proficient in|experienced with|knowledge of|hands-on with|skills:)\\s*", "");
            if (!clean.isEmpty()) {
                set.add(clean);
            }
        }
        return set;
    }

    private static boolean isMatch(String s1, String s2) {
        if (s1.equals(s2)) return true;
        String a1 = canonicalize(s1);
        String a2 = canonicalize(s2);
        if (a1.equals(a2)) return true;
        if (a1.contains(a2) || a2.contains(a1)) return true;
        return false;
    }

    private static String canonicalize(String s) {
        s = s.replaceAll("[^a-z0-9#+.]", "");
        if (s.equals("js")) return "javascript";
        if (s.equals("ts")) return "typescript";
        if (s.equals("reactjs")) return "react";
        if (s.equals("vuejs")) return "vue";
        if (s.equals("nodejs")) return "node";
        if (s.equals("postgres") || s.equals("postgresql")) return "postgres";
        if (s.equals("restapis") || s.equals("restful")) return "restapi";
        if (s.equals("k8s")) return "kubernetes";
        return s;
    }

    private static int parseMinExperience(String expStr) {
        if (expStr == null || expStr.trim().isEmpty()) return 0;
        String s = expStr.toLowerCase();
        if (s.contains("fresher") || s.contains("entry") || s.contains("0")) return 0;

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        if (text.length() == 1) return text.toUpperCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
