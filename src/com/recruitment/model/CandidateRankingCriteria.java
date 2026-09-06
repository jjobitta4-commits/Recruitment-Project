package com.recruitment.model;

/**
 * Core Innovation 3: Candidate Ranking Criteria enumeration and descriptors.
 * Supports multi-dimensional algorithmic candidate sorting for recruiters.
 */
public enum CandidateRankingCriteria {
    COMPOSITE_MATCH("match", "Smart Match Score (5-Factor Formula)"),
    EXPERIENCE_DEPTH("exp", "Experience Depth (Verified Years)"),
    TECHNICAL_SKILLS("skills", "Technical Skill Match"),
    ASSESSMENT_SCORE("assessment", "Adaptive Assessment Performance"),
    APPLICATION_DATE("date", "Application Recency (Newest First)");

    private final String code;
    private final String description;

    CandidateRankingCriteria(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CandidateRankingCriteria fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return COMPOSITE_MATCH;
        }
        String clean = text.trim().toLowerCase();
        if (clean.contains("exp")) {
            return EXPERIENCE_DEPTH;
        } else if (clean.contains("skill")) {
            return TECHNICAL_SKILLS;
        } else if (clean.contains("assess") || clean.contains("test")) {
            return ASSESSMENT_SCORE;
        } else if (clean.contains("date") || clean.contains("new")) {
            return APPLICATION_DATE;
        }
        return COMPOSITE_MATCH;
    }
}
