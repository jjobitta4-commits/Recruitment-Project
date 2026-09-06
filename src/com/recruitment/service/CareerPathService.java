package com.recruitment.service;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.CareerPathDAO;
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
 * Core Innovation 5: Career Path Recommendation Engine.
 * Evaluates candidate competencies across skills, experience, projects, education,
 * and adaptive assessments to map them to multi-milestone industry career ladders.
 *
 * Trajectory Formula:
 * TrajectoryScore = (SkillsOverlap * 0.40) + (ExpAlignment * 0.25) +
 *                   (ProjectRelevance * 0.15) + (Assessments * 0.10) + (EduRelevance * 0.10)
 */
public class CareerPathService {

    private final CareerPathDAO careerPathDAO = new CareerPathDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final JobDAO jobDAO = new JobDAO();

    /**
     * Generates personalized career path recommendations for a candidate, sorted by compatibility match score.
     */
    public List<CareerRecommendation> getRecommendationsForCandidate(int candidateId) {
        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        List<CareerPath> allPaths = careerPathDAO.getAllCareerPaths();
        List<Job> activeJobs = jobDAO.getAllActiveJobs();

        List<CareerRecommendation> recommendations = new ArrayList<>();
        if (candidate == null) {
            return recommendations;
        }

        for (CareerPath path : allPaths) {
            CareerRecommendation rec = evaluateCandidateForPath(candidate, path, activeJobs);
            recommendations.add(rec);
        }

        // Sort descending by match score
        recommendations.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return recommendations;
    }

    /**
     * Generates a detailed trajectory evaluation for a specific career path.
     */
    public CareerRecommendation getRecommendation(int candidateId, int pathId) {
        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        CareerPath path = careerPathDAO.getCareerPathById(pathId);
        if (candidate == null || path == null) return null;

        List<Job> activeJobs = jobDAO.getAllActiveJobs();
        return evaluateCandidateForPath(candidate, path, activeJobs);
    }

    /**
     * Evaluates candidate competencies against an industry career path.
     */
    public CareerRecommendation evaluateCandidateForPath(Candidate candidate, CareerPath path, List<Job> activeJobs) {
        CareerRecommendation rec = new CareerRecommendation();
        rec.setCandidateId(candidate.getCandidateId());
        rec.setCareerPath(path);

        List<CandidateSkill> candSkills = candidate.getSkills();
        if (candSkills == null) {
            candSkills = candidateDAO.getSkillsByCandidateId(candidate.getCandidateId());
        }

        // 1. Skills Overlap (40% weight)
        List<String> coreSkills = path.getCoreSkillsList();
        List<String> acquiredSkills = new ArrayList<>();
        List<String> allMissingSkills = new ArrayList<>();
        double skillPoints = 0;

        for (String reqSkill : coreSkills) {
            CandidateSkill cs = findCandidateSkill(candSkills, reqSkill);
            if (cs != null) {
                acquiredSkills.add(cs.getSkillName());
                skillPoints += getProficiencyMultiplier(cs.getProficiencyLevel());
            } else {
                allMissingSkills.add(reqSkill);
            }
        }

        int skillsScore = coreSkills.isEmpty() ? 100 : (int) Math.round((skillPoints / coreSkills.size()) * 100.0);
        skillsScore = Math.min(100, Math.max(10, skillsScore));
        rec.setSkillsScore(skillsScore);
        rec.setAcquiredSkills(acquiredSkills);
        rec.setAllMissingSkills(allMissingSkills);

        // 2. Experience Alignment (25% weight)
        double totalYears = calculateCandidateYearsExperience(candidate);
        int expScore = evaluateExperienceAlignment(totalYears, path.getMinStartingExperienceYears());
        rec.setExperienceScore(expScore);

        // 3. Project Relevance (15% weight)
        int projScore = evaluateProjectRelevance(candidate, coreSkills);
        rec.setProjectScore(projScore);

        // 4. Assessment Performance (10% weight)
        int assessScore = evaluateAssessmentMatch(candidate.getCandidateId());
        rec.setAssessmentScore(assessScore);

        // 5. Education Relevance (10% weight)
        int eduScore = evaluateEducationRelevance(candidate);
        rec.setEducationScore(eduScore);

        // Composite Weighted Trajectory Score
        double weighted = (skillsScore * 0.40) +
                          (expScore * 0.25) +
                          (projScore * 0.15) +
                          (assessScore * 0.10) +
                          (eduScore * 0.10);

        int finalScore = (int) Math.round(Math.min(100.0, Math.max(0.0, weighted)));
        rec.setMatchScore(finalScore);

        // Readiness Rating
        if (finalScore >= 75) {
            rec.setReadinessRating("HIGH_AFFINITY");
            rec.setReadinessLabel("High Career Match");
        } else if (finalScore >= 50) {
            rec.setReadinessRating("MODERATE_AFFINITY");
            rec.setReadinessLabel("Moderate Affinity");
        } else {
            rec.setReadinessRating("EXPLORATORY");
            rec.setReadinessLabel("Exploratory Transition");
        }

        // 6. Multi-Milestone Ladder Evaluation (Levels 1 to 4)
        evaluateMilestoneProgression(candidate, path, rec, totalYears, candSkills);

        // 7. Stepping-Stone Active Vacancies
        List<Job> steppingStones = findSteppingStoneJobs(path, activeJobs);
        rec.setMatchingJobs(steppingStones);

        // 8. Human-Readable Trajectory Summary
        String targetLevelName = rec.getNextMilestone() != null ? rec.getNextMilestone().getLevelName() : "Apex Mastery";
        String nextFrontierStr = rec.getNextFrontierSkills().isEmpty() ? "None (Core Competency Met)" : String.join(", ", rec.getNextFrontierSkills());

        String summary = String.format(
            "Trajectory affinity: %d%% (%s). Current Standing: %s. Target Milestone: %s. " +
            "Key skills to acquire next: %s.",
            finalScore, rec.getReadinessLabel(),
            rec.getCurrentMilestone() != null ? rec.getCurrentMilestone().getLevelName() : "Foundation Candidate",
            targetLevelName, nextFrontierStr
        );
        rec.setTrajectorySummary(summary);

        return rec;
    }

    /**
     * Evaluates candidate's current progress across the 4 milestones of the career ladder.
     */
    private void evaluateMilestoneProgression(Candidate candidate, CareerPath path, CareerRecommendation rec,
                                              double totalYears, List<CandidateSkill> candSkills) {
        List<CareerMilestone> milestones = path.getMilestones();
        List<CareerMilestone> completed = new ArrayList<>();
        List<String> nextFrontierSkills = new ArrayList<>();

        int currentLevelOrder = 1;

        for (CareerMilestone m : milestones) {
            List<String> reqSkills = m.getSkillsList();
            List<String> mAcquired = new ArrayList<>();
            List<String> mMissing = new ArrayList<>();

            for (String s : reqSkills) {
                if (findCandidateSkill(candSkills, s) != null) {
                    mAcquired.add(s);
                } else {
                    mMissing.add(s);
                }
            }

            m.setAcquiredSkills(mAcquired);
            m.setMissingSkills(mMissing);

            // Level completion criteria: Candidate has >= 60% of milestone skills AND appropriate years
            double minYears = getMinYearsForLevel(m.getLevelOrder());
            boolean skillsSufficient = reqSkills.isEmpty() || ((double) mAcquired.size() / reqSkills.size()) >= 0.60;
            boolean expSufficient = totalYears >= (minYears * 0.65);

            if (skillsSufficient && expSufficient) {
                m.setCompleted(true);
                completed.add(m);
                if (m.getLevelOrder() >= currentLevelOrder) {
                    currentLevelOrder = Math.min(4, m.getLevelOrder() + 1);
                }
            } else {
                m.setCompleted(false);
            }
        }

        // Determine current and target milestone
        CareerMilestone currentMilestone = null;
        CareerMilestone nextMilestone = null;

        for (CareerMilestone m : milestones) {
            if (m.getLevelOrder() == currentLevelOrder) {
                currentMilestone = m;
                m.setCurrent(true);
            } else if (m.getLevelOrder() == currentLevelOrder + 1) {
                nextMilestone = m;
                m.setTarget(true);
            } else if (m.getLevelOrder() > currentLevelOrder) {
                m.setTarget(false);
            }
        }

        // If candidate completed all, current is highest, next is top
        if (currentMilestone == null && !milestones.isEmpty()) {
            currentMilestone = milestones.get(milestones.size() - 1);
            currentMilestone.setCurrent(true);
            nextMilestone = currentMilestone;
        }

        // The target milestone defines next frontier skills
        CareerMilestone targetM = nextMilestone != null ? nextMilestone : currentMilestone;
        if (targetM != null) {
            nextFrontierSkills.addAll(targetM.getMissingSkills());
        }

        rec.setCurrentMilestoneLevel(currentLevelOrder);
        rec.setCurrentMilestone(currentMilestone);
        rec.setNextMilestone(nextMilestone != null ? nextMilestone : currentMilestone);
        rec.setCompletedMilestones(completed);
        rec.setNextFrontierSkills(nextFrontierSkills);
    }

    private double getMinYearsForLevel(int levelOrder) {
        switch (levelOrder) {
            case 1: return 0.0;
            case 2: return 2.0;
            case 3: return 5.0;
            case 4: return 8.0;
            default: return 0.0;
        }
    }

    private List<Job> findSteppingStoneJobs(CareerPath path, List<Job> activeJobs) {
        List<Job> matched = new ArrayList<>();
        if (activeJobs == null || activeJobs.isEmpty()) return matched;

        List<String> coreSkills = path.getCoreSkillsList();
        String pathTitleLower = path.getTitle().toLowerCase();

        for (Job job : activeJobs) {
            String jobTitleLower = job.getTitle().toLowerCase();
            boolean titleRelevant = false;
            if (pathTitleLower.contains("backend") && jobTitleLower.contains("backend")) titleRelevant = true;
            if (pathTitleLower.contains("full-stack") && jobTitleLower.contains("full stack")) titleRelevant = true;
            if (pathTitleLower.contains("devops") && (jobTitleLower.contains("devops") || jobTitleLower.contains("cloud"))) titleRelevant = true;
            if (pathTitleLower.contains("data") && (jobTitleLower.contains("data") || jobTitleLower.contains("analyst"))) titleRelevant = true;
            if (jobTitleLower.contains("java") && pathTitleLower.contains("backend")) titleRelevant = true;

            int skillMatches = 0;
            if (job.getJobSkills() != null) {
                for (JobSkill js : job.getJobSkills()) {
                    for (String cs : coreSkills) {
                        if (canonicalize(js.getSkillName()).equals(canonicalize(cs))) {
                            skillMatches++;
                            break;
                        }
                    }
                }
            }

            if (titleRelevant || skillMatches >= 2) {
                matched.add(job);
                if (matched.size() >= 3) break;
            }
        }
        return matched;
    }

    private double calculateCandidateYearsExperience(Candidate candidate) {
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

        if (candYears < 0.5 && candidate.getSkills() != null) {
            for (CandidateSkill cs : candidate.getSkills()) {
                if (cs.getYearsExperience() > candYears) {
                    candYears = cs.getYearsExperience();
                }
            }
        }
        return candYears;
    }

    private int evaluateExperienceAlignment(double candYears, int startingYears) {
        if (candYears >= 6.0) return 100;
        if (candYears >= 4.0) return 92;
        if (candYears >= 2.0) return 85;
        if (candYears >= 1.0) return 75;
        return 65;
    }

    private int evaluateProjectRelevance(Candidate candidate, List<String> coreSkills) {
        List<Project> projects = candidate.getProjects();
        if (projects == null || projects.isEmpty()) return 40;
        if (coreSkills == null || coreSkills.isEmpty()) return 90;

        int matchedCount = 0;
        for (String skill : coreSkills) {
            String skillKey = canonicalize(skill);
            for (Project p : projects) {
                String stack = canonicalize(p.getTechnologiesUsed() != null ? p.getTechnologiesUsed() : "");
                String desc = canonicalize(p.getDescription() != null ? p.getDescription() : "");
                if (stack.contains(skillKey) || desc.contains(skillKey)) {
                    matchedCount++;
                    break;
                }
            }
        }

        double ratio = (double) matchedCount / coreSkills.size();
        return (int) Math.round(Math.min(100.0, 45.0 + (ratio * 55.0)));
    }

    private int evaluateEducationRelevance(Candidate candidate) {
        List<Education> edus = candidate.getEducation();
        if (edus == null || edus.isEmpty()) return 65;

        int highest = 60;
        for (Education edu : edus) {
            String deg = (edu.getDegree() != null ? edu.getDegree() : "").toLowerCase();
            String field = (edu.getFieldOfStudy() != null ? edu.getFieldOfStudy() : "").toLowerCase();

            int score = 65;
            if (deg.contains("ph.d") || deg.contains("doctor")) score = 100;
            else if (deg.contains("master") || deg.contains("m.s") || deg.contains("m.tech")) score = 90;
            else if (deg.contains("bachelor") || deg.contains("b.s") || deg.contains("b.tech") || deg.contains("b.e")) score = 85;

            if (field.contains("computer") || field.contains("software") || field.contains("information") ||
                field.contains("data") || field.contains("engineering") || field.contains("tech")) {
                score = Math.min(100, score + 10);
            }
            if (score > highest) highest = score;
        }
        return highest;
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
            System.err.println("[CareerPathService.evaluateAssessmentMatch] Error: " + e.getMessage());
        }
        return 75; // Default baseline score
    }

    private CandidateSkill findCandidateSkill(List<CandidateSkill> candSkills, String skillName) {
        if (candSkills == null || skillName == null) return null;
        String target = canonicalize(skillName);
        for (CandidateSkill cs : candSkills) {
            String candName = canonicalize(cs.getSkillName());
            if (candName.equals(target) || candName.contains(target) || target.contains(candName)) {
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
