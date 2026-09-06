package com.recruitment.service;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.JobDAO;
import com.recruitment.model.*;
import com.recruitment.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * Core Innovation 1: Smart Job Matching Algorithm.
 * Implements the mathematical 5-factor weighted formula:
 * Score = (SkillMatch * 0.45) + (ExpMatch * 0.20) + (EduMatch * 0.10) + (ProjectMatch * 0.10) + (AssessmentMatch * 0.15)
 */
public class JobMatchingService {

    private final JobDAO jobDAO = new JobDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();

    /**
     * Computes the complete 5-factor compatibility match between a candidate and a job.
     */
    public MatchResult calculateMatch(Candidate candidate, Job job) {
        MatchResult result = new MatchResult();
        if (candidate == null || job == null) {
            result.setOverallScore(0);
            result.setMatchLevel("LOW");
            result.setMatchSummary("Candidate or Job profile not available.");
            return result;
        }

        result.setJobId(job.getJobId());
        result.setCandidateId(candidate.getCandidateId());

        // 1. Skill Match (45%)
        List<JobSkill> jobSkills = job.getJobSkills();
        if (jobSkills == null || jobSkills.isEmpty()) {
            jobSkills = jobDAO.getSkillsForJob(null, job.getJobId());
        }

        List<CandidateSkill> candSkills = candidate.getSkills();
        if (candSkills == null) {
            candSkills = candidateDAO.getSkillsByCandidateId(candidate.getCandidateId());
        }

        SkillMatchBreakdown skillBreakdown = evaluateSkillMatch(candSkills, jobSkills);
        result.setSkillScore(skillBreakdown.score);
        result.setMatchedSkills(skillBreakdown.matched);
        result.setMissingSkills(skillBreakdown.missing);
        result.setMissingMandatorySkills(skillBreakdown.missingMandatory);
        result.setMissingPreferredSkills(skillBreakdown.missingPreferred);
        result.setMandatoryPrerequisitesMet(skillBreakdown.missingMandatory.isEmpty());

        // 2. Experience Match (20%)
        int expScore = evaluateExperienceMatch(candidate, job);
        result.setExperienceScore(expScore);

        // 3. Education Match (10%)
        int eduScore = evaluateEducationMatch(candidate, job);
        result.setEducationScore(eduScore);

        // 4. Project Match (10%)
        int projScore = evaluateProjectMatch(candidate, jobSkills);
        result.setProjectScore(projScore);

        // 5. Assessment Match (15%)
        int assessScore = evaluateAssessmentMatch(candidate.getCandidateId());
        result.setAssessmentScore(assessScore);

        // Weighted Formula Computation
        double weightedScore = (skillBreakdown.score * 0.45) +
                (expScore * 0.20) +
                (eduScore * 0.10) +
                (projScore * 0.10) +
                (assessScore * 0.15);

        // Penalty constraint: If mandatory prerequisite skills are missing, cap at 75%
        if (!skillBreakdown.missingMandatory.isEmpty() && weightedScore > 75.0) {
            weightedScore = 75.0;
        }

        int finalScore = (int) Math.round(Math.min(100.0, Math.max(0.0, weightedScore)));
        result.setOverallScore(finalScore);

        // Qualitative Categorization
        String level;
        if (finalScore >= 85) {
            level = "EXCELLENT";
        } else if (finalScore >= 70) {
            level = "STRONG";
        } else if (finalScore >= 50) {
            level = "MODERATE";
        } else {
            level = "LOW";
        }
        result.setMatchLevel(level);

        // Generate Human-Readable Summary
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Compatibility rating: %d%% (%s). ", finalScore, level));
        summary.append(String.format("Skills: %d/100 (45%% wt), Experience: %d/100 (20%% wt), Education: %d/100 (10%% wt), Projects: %d/100 (10%% wt), Assessments: %d/100 (15%% wt). ",
                skillBreakdown.score, expScore, eduScore, projScore, assessScore));

        if (!skillBreakdown.missingMandatory.isEmpty()) {
            summary.append(String.format("Missing %d critical mandatory prerequisite(s): %s.",
                    skillBreakdown.missingMandatory.size(), String.join(", ", skillBreakdown.missingMandatory)));
        } else {
            summary.append("All mandatory technical prerequisites are satisfied.");
        }
        result.setMatchSummary(summary.toString());

        return result;
    }

    public MatchResult calculateMatch(int candidateId, int jobId) {
        Candidate c = candidateDAO.getCandidateById(candidateId);
        Job j = jobDAO.getJobById(jobId);
        return calculateMatch(c, j);
    }

    public MatchResult calculateMatchForUser(int userId, int jobId) {
        Candidate c = candidateDAO.getCandidateByUserId(userId);
        Job j = jobDAO.getJobById(jobId);
        return calculateMatch(c, j);
    }

    /**
     * Ranks all active jobs for a candidate based on the 5-factor smart matching formula.
     */
    public List<Job> getRankedJobsForCandidate(int candidateId) {
        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        List<Job> activeJobs = jobDAO.getAllActiveJobs();
        if (candidate == null) return activeJobs;

        for (Job job : activeJobs) {
            MatchResult mr = calculateMatch(candidate, job);
            job.setMatchScore(mr.getOverallScore());
            job.setMatchLevel(mr.getMatchLevel());
            job.setMatchedSkills(mr.getMatchedSkills());
            job.setMissingSkills(mr.getMissingSkills());
        }

        activeJobs.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return activeJobs;
    }

    // ==========================================
    // Dimension Evaluators
    // ==========================================

    private static class SkillMatchBreakdown {
        int score = 0;
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> missingMandatory = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();
    }

    private SkillMatchBreakdown evaluateSkillMatch(List<CandidateSkill> candSkills, List<JobSkill> jobSkills) {
        SkillMatchBreakdown b = new SkillMatchBreakdown();
        if (jobSkills == null || jobSkills.isEmpty()) {
            b.score = 100;
            return b;
        }

        List<JobSkill> mandatory = new ArrayList<>();
        List<JobSkill> preferred = new ArrayList<>();
        for (JobSkill js : jobSkills) {
            if (js.isMandatory()) mandatory.add(js);
            else preferred.add(js);
        }

        double mandatoryPoints = 0;
        for (JobSkill req : mandatory) {
            CandidateSkill cs = findCandidateSkill(candSkills, req.getSkillName());
            if (cs != null) {
                b.matched.add(req.getSkillName());
                double profMult = getProficiencyMultiplier(cs.getProficiencyLevel());
                double expRatio = req.getMinYearsRequired() > 0
                        ? Math.min(1.0, Math.max(0.5, (double) cs.getYearsExperience() / req.getMinYearsRequired()))
                        : 1.0;
                mandatoryPoints += (profMult * expRatio);
            } else {
                b.missing.add(req.getSkillName());
                b.missingMandatory.add(req.getSkillName());
            }
        }

        double preferredPoints = 0;
        for (JobSkill req : preferred) {
            CandidateSkill cs = findCandidateSkill(candSkills, req.getSkillName());
            if (cs != null) {
                b.matched.add(req.getSkillName());
                double profMult = getProficiencyMultiplier(cs.getProficiencyLevel());
                preferredPoints += profMult;
            } else {
                b.missing.add(req.getSkillName());
                b.missingPreferred.add(req.getSkillName());
            }
        }

        double mandScore = mandatory.isEmpty() ? 100.0 : (mandatoryPoints / mandatory.size()) * 100.0;
        double prefScore = preferred.isEmpty() ? 100.0 : (preferredPoints / preferred.size()) * 100.0;

        double finalSkillScore;
        if (preferred.isEmpty()) {
            finalSkillScore = mandScore;
        } else if (mandatory.isEmpty()) {
            finalSkillScore = prefScore;
        } else {
            finalSkillScore = (mandScore * 0.70) + (prefScore * 0.30);
        }

        b.score = (int) Math.round(Math.min(100.0, Math.max(0.0, finalSkillScore)));
        return b;
    }

    private CandidateSkill findCandidateSkill(List<CandidateSkill> candSkills, String skillName) {
        if (candSkills == null || skillName == null) return null;
        String target = canonicalize(skillName);
        for (CandidateSkill cs : candSkills) {
            if (canonicalize(cs.getSkillName()).equals(target) ||
                canonicalize(cs.getSkillName()).contains(target) ||
                target.contains(canonicalize(cs.getSkillName()))) {
                return cs;
            }
        }
        return null;
    }

    private double getProficiencyMultiplier(String level) {
        if (level == null) return 0.85;
        switch (level.toLowerCase()) {
            case "expert": return 1.0;
            case "advanced": return 0.95;
            case "intermediate": return 0.85;
            case "beginner": return 0.65;
            default: return 0.80;
        }
    }

    private int evaluateExperienceMatch(Candidate candidate, Job job) {
        int reqYears = job.getMinExperienceYears();
        if (reqYears <= 0) return 100;

        double candYears = 0;
        List<Experience> exps = candidate.getExperience();
        if (exps != null && !exps.isEmpty()) {
            for (Experience exp : exps) {
                if (exp.getStartDate() != null) {
                    LocalDate start = exp.getStartDate().toLocalDate();
                    LocalDate end = (exp.isCurrent() || exp.getEndDate() == null)
                            ? LocalDate.now()
                            : exp.getEndDate().toLocalDate();
                    Period period = Period.between(start, end);
                    candYears += (period.getYears() + (period.getMonths() / 12.0));
                }
            }
        }

        // Fallback to highest years experience from declared skills
        if (candYears < 0.5 && candidate.getSkills() != null) {
            for (CandidateSkill cs : candidate.getSkills()) {
                if (cs.getYearsExperience() > candYears) {
                    candYears = cs.getYearsExperience();
                }
            }
        }

        if (candYears >= reqYears) return 100;
        double ratio = (candYears / reqYears) * 100.0;
        return (int) Math.round(Math.min(100.0, Math.max(15.0, ratio)));
    }

    private int evaluateEducationMatch(Candidate candidate, Job job) {
        List<Education> edus = candidate.getEducation();
        if (edus == null || edus.isEmpty()) return 60; // Baseline

        int highestScore = 50;
        for (Education edu : edus) {
            String deg = (edu.getDegree() != null ? edu.getDegree() : "").toLowerCase();
            String field = (edu.getFieldOfStudy() != null ? edu.getFieldOfStudy() : "").toLowerCase();

            int current = 50;
            if (deg.contains("ph.d") || deg.contains("doctor")) current = 100;
            else if (deg.contains("master") || deg.contains("m.s") || deg.contains("m.tech") || deg.contains("mca")) current = 90;
            else if (deg.contains("bachelor") || deg.contains("b.s") || deg.contains("b.tech") || deg.contains("b.e") || deg.contains("bca")) current = 80;
            else if (deg.contains("diploma") || deg.contains("associate")) current = 65;

            // Relevant field of study bonus
            if (field.contains("computer") || field.contains("software") || field.contains("information") ||
                field.contains("data") || field.contains("engineering") || field.contains("tech")) {
                current = Math.min(100, current + 10);
            }

            if (current > highestScore) highestScore = current;
        }

        return highestScore;
    }

    private int evaluateProjectMatch(Candidate candidate, List<JobSkill> jobSkills) {
        List<Project> projects = candidate.getProjects();
        if (projects == null || projects.isEmpty()) return 40;
        if (jobSkills == null || jobSkills.isEmpty()) return 100;

        int matchedCount = 0;
        for (JobSkill js : jobSkills) {
            String skillKey = canonicalize(js.getSkillName());
            boolean applied = false;
            for (Project p : projects) {
                String stack = canonicalize(p.getTechnologiesUsed() != null ? p.getTechnologiesUsed() : "");
                String desc = canonicalize(p.getDescription() != null ? p.getDescription() : "");
                if (stack.contains(skillKey) || desc.contains(skillKey)) {
                    applied = true;
                    break;
                }
            }
            if (applied) matchedCount++;
        }

        double ratio = (double) matchedCount / jobSkills.size();
        return (int) Math.round(Math.min(100.0, 50.0 + (ratio * 50.0)));
    }

    private int evaluateAssessmentMatch(int candidateId) {
        String sql = "SELECT total_score, max_score FROM assessment_attempts WHERE candidate_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                double total = 0;
                int count = 0;
                while (rs.next()) {
                    int sc = rs.getInt("total_score");
                    int max = rs.getInt("max_score");
                    if (max > 0) {
                        total += ((double) sc / max) * 100.0;
                        count++;
                    }
                }
                if (count > 0) {
                    return (int) Math.round(total / count);
                }
            }
        } catch (Exception e) {
            System.err.println("[JobMatchingService.evaluateAssessmentMatch] Error: " + e.getMessage());
        }

        // Baseline assessment score for candidates who have not yet taken a quiz
        return 75;
    }

    private String canonicalize(String text) {
        if (text == null) return "";
        String s = text.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (s.equals("js")) return "javascript";
        if (s.equals("ts")) return "typescript";
        if (s.equals("k8s")) return "kubernetes";
        if (s.equals("postgres") || s.equals("postgresql")) return "postgres";
        return s;
    }
}
