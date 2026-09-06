package com.recruitment.service;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.model.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service Layer for Candidate profiles, normalized portfolios, profile completeness calculation,
 * and the Resume Technical Keyword Extractor.
 */
public class CandidateService {

    private final CandidateDAO candidateDAO = new CandidateDAO();

    /**
     * Calculates dynamic profile completeness (0-100%) across 5 weighted dimensions:
     * - Personal Information & Bio (25%)
     * - Declared Technical Skills (25%)
     * - Education Background (20%)
     * - Work Experience (15%)
     * - Portfolio Projects (15%)
     */
    public int calculateCompleteness(Candidate c) {
        if (c == null) return 0;
        int score = 0;

        // 1. Personal & Contact Info (25%)
        if (c.getFullName() != null && !c.getFullName().trim().isEmpty()) score += 5;
        if (c.getPhone() != null && !c.getPhone().trim().isEmpty()) score += 5;
        if (c.getCity() != null && !c.getCity().trim().isEmpty()) score += 5;
        if (c.getCountry() != null && !c.getCountry().trim().isEmpty()) score += 5;
        if (c.getBio() != null && c.getBio().trim().length() > 10) score += 5;

        // 2. Technical Skills (25%)
        List<CandidateSkill> skills = c.getSkills();
        if (skills != null) {
            if (skills.size() >= 5) score += 25;
            else if (skills.size() >= 3) score += 18;
            else if (!skills.isEmpty()) score += 10;
        }

        // 3. Education (20%)
        List<Education> edu = c.getEducation();
        if (edu != null && !edu.isEmpty()) {
            score += Math.min(20, edu.size() * 10);
        }

        // 4. Experience (15%)
        List<Experience> exp = c.getExperience();
        if (exp != null && !exp.isEmpty()) {
            score += Math.min(15, exp.size() * 8);
        }

        // 5. Projects (15%)
        List<Project> proj = c.getProjects();
        if (proj != null && !proj.isEmpty()) {
            score += Math.min(15, proj.size() * 8);
        }

        int finalScore = Math.min(100, score);
        candidateDAO.updateProfileCompletion(c.getCandidateId(), finalScore);
        c.setProfileCompletion(finalScore);
        return finalScore;
    }

    /**
     * Resume Technical Keyword Extractor:
     * Scans raw text against the master taxonomy of technical skills in MySQL.
     * Accurately extracts recognized technologies, frameworks, databases, and tools.
     */
    public Map<String, Object> extractKeywordsFromResume(String rawText) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            result.put("totalFound", 0);
            result.put("detectedSkills", Collections.emptyList());
            return result;
        }

        String lowerText = rawText.toLowerCase();
        List<Skill> masterSkills = candidateDAO.getAllMasterSkills();
        List<Map<String, Object>> detected = new ArrayList<>();
        Set<String> addedNames = new HashSet<>();

        // Common industry aliases mapping
        Map<String, String> aliases = new HashMap<>();
        aliases.put("js", "javascript");
        aliases.put("ts", "typescript");
        aliases.put("postgres", "postgresql");
        aliases.put("k8s", "kubernetes");
        aliases.put("py", "python");

        for (Skill s : masterSkills) {
            String skillName = s.getName().trim();
            String skillLower = skillName.toLowerCase();

            // Word-boundary regex matching
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skillLower) + "\\b", Pattern.CASE_INSENSITIVE);
            boolean matched = pattern.matcher(lowerText).find();

            if (!matched && aliases.containsKey(skillLower)) {
                Pattern aliasPattern = Pattern.compile("\\b" + Pattern.quote(aliases.get(skillLower)) + "\\b", Pattern.CASE_INSENSITIVE);
                matched = aliasPattern.matcher(lowerText).find();
            }

            if (matched && addedNames.add(skillName)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("skillId", s.getSkillId());
                item.put("name", s.getName());
                item.put("category", s.getCategory());
                detected.add(item);
            }
        }

        result.put("totalFound", detected.size());
        result.put("detectedSkills", detected);
        result.put("rawTextLength", rawText.length());
        return result;
    }

    public Candidate getCandidate(int userId) {
        Candidate c = candidateDAO.getCandidateByUserId(userId);
        if (c != null) {
            calculateCompleteness(c);
        }
        return c;
    }

    public boolean updateProfile(Candidate c) {
        calculateCompleteness(c);
        return candidateDAO.updateCandidateProfile(c);
    }
}
