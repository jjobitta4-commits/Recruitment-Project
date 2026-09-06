package com.recruitment.service;

import com.recruitment.dao.ApplicationDAO;
import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.JobDAO;
import com.recruitment.dao.NotificationDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.model.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Service Layer for Job Application Lifecycle & Candidate Ranking System (Core Innovation 3).
 * Integrates with the 5-factor Smart Job Matching Algorithm (Core Innovation 1).
 */
public class ApplicationService {

    private final ApplicationDAO applicationDAO = new ApplicationDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final JobDAO jobDAO = new JobDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final JobMatchingService matchingService = new JobMatchingService();

    /**
     * Submits a candidate job application:
     * 1. Validates candidate & job validity
     * 2. Enforces duplicate application prevention
     * 3. Executes 5-factor matching algorithm & stores composite score
     * 4. Persists application and auto-triggers notifications
     */
    public Application applyForJob(int candidateId, int jobId, String coverLetter, Integer resumeId, String resumeFileName) {
        Candidate candidate = candidateDAO.getCandidateById(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate profile not found.");
        }

        Job job = jobDAO.getJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Target job opening not found.");
        }

        if (!"Active".equalsIgnoreCase(job.getStatus())) {
            throw new IllegalArgumentException("This job posting is closed for applications.");
        }

        if (applicationDAO.hasApplied(candidateId, jobId)) {
            throw new IllegalStateException("You have already submitted an application for this job opening.");
        }

        // Core Innovation 1: Calculate real-time 5-factor match score
        MatchResult matchResult = matchingService.calculateMatch(candidate, job);
        int computedScore = matchResult.getOverallScore();

        // Resolve or register resume
        if ((resumeId == null || resumeId <= 0) && resumeFileName != null && !resumeFileName.trim().isEmpty()) {
            int savedResumeId = applicationDAO.saveResume(candidateId, resumeFileName, "resumes/" + resumeFileName);
            if (savedResumeId > 0) {
                resumeId = savedResumeId;
            }
        }
        if (resumeId == null || resumeId <= 0) {
            resumeId = applicationDAO.getCandidateLatestResumeId(candidateId);
        }

        Application app = new Application();
        app.setJobId(jobId);
        app.setCandidateId(candidateId);
        app.setResumeId(resumeId);
        app.setCoverLetter(coverLetter != null ? coverLetter.trim() : "");
        app.setMatchScore(computedScore);
        app.setStatus("Applied");

        boolean success = applicationDAO.applyForJob(app);
        if (!success) {
            throw new RuntimeException("Failed to persist job application.");
        }

        // Enrich application with full match analytics
        enrichApplicationWithMatch(app, matchResult, candidate);

        // Auto-trigger notifications
        try {
            // Notification for Candidate
            notificationDAO.createNotification(
                    candidate.getUserId(),
                    "Application Submitted",
                    String.format("You successfully submitted your application for '%s' at %s. Smart Match Score: %d%% (%s).",
                            job.getTitle(), job.getCompany(), computedScore, matchResult.getMatchLevel())
            );

            // Notification for Recruiter
            Recruiter recruiter = recruiterDAO.getRecruiterById(job.getRecruiterId());
            if (recruiter != null) {
                notificationDAO.createNotification(
                        recruiter.getUserId(),
                        "New Application Received",
                        String.format("%s applied for '%s'. Smart Match Score: %d%% (%s).",
                                candidate.getFullName(), job.getTitle(), computedScore, matchResult.getMatchLevel())
                );
            }
        } catch (Exception notifErr) {
            System.err.println("[ApplicationService] Notification notice: " + notifErr.getMessage());
        }

        return app;
    }

    /**
     * Retrieves all applications submitted by a candidate with match breakdown and hiring progression.
     */
    public List<Application> getApplicationsForCandidate(int candidateId) {
        List<Application> apps = applicationDAO.getApplicationsByCandidate(candidateId);
        Candidate candidate = candidateDAO.getCandidateById(candidateId);

        for (Application app : apps) {
            try {
                Job job = jobDAO.getJobById(app.getJobId());
                if (candidate != null && job != null) {
                    MatchResult match = matchingService.calculateMatch(candidate, job);
                    enrichApplicationWithMatch(app, match, candidate);
                }
            } catch (Exception ignored) {}
        }
        return apps;
    }

    /**
     * Core Innovation 3: Candidate Ranking System for Recruiters.
     * Evaluates all candidate applications against vacancy prerequisites,
     * computes full 5-factor compatibility metrics, applies multi-criteria sorting,
     * assigns dynamic rank position (1, 2, 3...), and attaches intelligent insights.
     *
     * @param recruiterId Recruiter ID
     * @param jobId Optional job filter (null or <=0 for all recruiter jobs)
     * @param statusFilter Optional status filter ("All", "Applied", "Under_Review", etc.)
     * @param sortBy Ranking criteria ("match", "exp", "skills", "assessment", "date")
     * @return Ordered list of ranked candidate applications
     */
    public List<Application> rankCandidatesForJob(int recruiterId, Integer jobId, String statusFilter, String sortBy) {
        List<Application> apps = applicationDAO.getApplicationsByRecruiter(recruiterId, jobId, statusFilter);
        if (apps.isEmpty()) {
            return apps;
        }

        CandidateRankingCriteria criteria = CandidateRankingCriteria.fromString(sortBy);

        // Pre-fetch & compute match breakdowns and experience depth for each candidate
        for (Application app : apps) {
            try {
                Candidate cand = candidateDAO.getCandidateById(app.getCandidateId());
                Job job = jobDAO.getJobById(app.getJobId());

                if (cand != null && job != null) {
                    MatchResult match = matchingService.calculateMatch(cand, job);
                    enrichApplicationWithMatch(app, match, cand);
                }
            } catch (Exception e) {
                System.err.println("[ApplicationService.rankCandidates] Notice: " + e.getMessage());
            }
        }

        // Multi-Criteria Algorithmic Comparator
        Comparator<Application> comparator;
        switch (criteria) {
            case EXPERIENCE_DEPTH:
                comparator = Comparator
                        .comparingInt(Application::getCandidateExperienceYears)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(Application::getMatchScore).reversed())
                        .thenComparing(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                break;

            case TECHNICAL_SKILLS:
                comparator = Comparator
                        .comparingInt(Application::getSkillScore)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(Application::getMatchScore).reversed())
                        .thenComparing(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                break;

            case ASSESSMENT_SCORE:
                comparator = Comparator
                        .comparingInt(Application::getAssessmentScore)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(Application::getMatchScore).reversed())
                        .thenComparing(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                break;

            case APPLICATION_DATE:
                comparator = Comparator
                        .comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingInt(Application::getMatchScore).reversed());
                break;

            case COMPOSITE_MATCH:
            default:
                comparator = Comparator
                        .comparingInt(Application::getMatchScore)
                        .reversed()
                        .thenComparing(Comparator.comparingInt(Application::getCandidateExperienceYears).reversed())
                        .thenComparing(Comparator.comparing(Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
        }

        apps.sort(comparator);

        // Assign Dynamic 1-based Ranks & Format Ranking Insights
        for (int i = 0; i < apps.size(); i++) {
            Application a = apps.get(i);
            int rankNum = i + 1;
            a.setRank(rankNum);

            StringBuilder insight = new StringBuilder();
            if (rankNum == 1) {
                insight.append("🏆 Rank #1 Top Candidate");
            } else if (rankNum == 2) {
                insight.append("🥈 Rank #2 Candidate");
            } else if (rankNum == 3) {
                insight.append("🥉 Rank #3 Candidate");
            } else {
                insight.append("Rank #").append(rankNum);
            }

            insight.append(" • Match: ").append(a.getMatchScore()).append("% (").append(a.getMatchLevel()).append(")");
            insight.append(" • Exp: ").append(a.getCandidateExperienceYears()).append(" Yrs");

            if (a.isMandatoryPrerequisitesMet()) {
                insight.append(" • ✅ All Prerequisites Met");
            } else if (!a.getMissingMandatorySkills().isEmpty()) {
                insight.append(" • ⚠️ Missing ").append(a.getMissingMandatorySkills().size()).append(" Mandatory Skill(s)");
            }

            a.setRankingInsight(insight.toString());
        }

        return apps;
    }

    /**
     * Updates an application status and notifies the candidate.
     */
    public boolean updateApplicationStatus(int recruiterId, int applicationId, String newStatus) {
        Application app = applicationDAO.getApplicationById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("Application not found.");
        }

        if (app.getRecruiterId() != recruiterId) {
            throw new SecurityException("Access denied. You do not own the job for this application.");
        }

        String dbStatus = ApplicationDAO.normalizeStatusForDb(newStatus);
        boolean updated = applicationDAO.updateApplicationStatus(applicationId, dbStatus);
        if (updated) {
            try {
                Candidate cand = candidateDAO.getCandidateById(app.getCandidateId());
                if (cand != null) {
                    String displayStatus = ApplicationDAO.formatStatusForDisplay(dbStatus);
                    notificationDAO.createNotification(
                            cand.getUserId(),
                            "Application Status Updated",
                            String.format("Your application for '%s' at %s has been updated to: %s.",
                                    app.getJobTitle(), app.getCompanyName(), displayStatus)
                    );
                }
            } catch (Exception ignored) {}
        }
        return updated;
    }

    /**
     * Candidate withdraws an application in early stages ('Applied' or 'Under_Review').
     */
    public boolean withdrawApplication(int candidateId, int applicationId) {
        Application app = applicationDAO.getApplicationById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("Application not found.");
        }

        if (app.getCandidateId() != candidateId) {
            throw new SecurityException("Access denied. This application does not belong to you.");
        }

        String status = app.getStatus();
        if (!"Applied".equalsIgnoreCase(status) && !"Under Review".equalsIgnoreCase(status) && !"Under_Review".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Applications that have progressed past initial review cannot be withdrawn.");
        }

        boolean withdrawn = applicationDAO.withdrawApplication(applicationId, candidateId);
        if (withdrawn) {
            try {
                Candidate cand = candidateDAO.getCandidateById(candidateId);
                if (cand != null) {
                    notificationDAO.createNotification(
                            cand.getUserId(),
                            "Application Withdrawn",
                            String.format("You have withdrawn your application for '%s' at %s.",
                                    app.getJobTitle(), app.getCompanyName())
                    );
                }
            } catch (Exception ignored) {}
        }
        return withdrawn;
    }

    /**
     * Retrieves application statistics for recruiter.
     */
    public Map<String, Integer> getApplicationStatsForRecruiter(int recruiterId) {
        return applicationDAO.getApplicationStatsForRecruiter(recruiterId);
    }

    /**
     * Retrieves application statistics for candidate.
     */
    public Map<String, Integer> getApplicationStatsForCandidate(int candidateId) {
        return applicationDAO.getApplicationStatsForCandidate(candidateId);
    }

    /**
     * Enriches an Application instance with 5-factor match score components and candidate portfolio metrics.
     */
    private void enrichApplicationWithMatch(Application app, MatchResult match, Candidate cand) {
        if (match != null) {
            app.setMatchScore(match.getOverallScore());
            app.setMatchLevel(match.getMatchLevel());
            app.setSkillScore(match.getSkillScore());
            app.setExperienceScore(match.getExperienceScore());
            app.setEducationScore(match.getEducationScore());
            app.setProjectScore(match.getProjectScore());
            app.setAssessmentScore(match.getAssessmentScore());
            app.setMatchedSkills(match.getMatchedSkills());
            app.setMissingSkills(match.getMissingSkills());
            app.setMissingMandatorySkills(match.getMissingMandatorySkills());
            app.setMissingPreferredSkills(match.getMissingPreferredSkills());
            app.setMandatoryPrerequisitesMet(match.isMandatoryPrerequisitesMet());
        }

        if (cand != null) {
            // Calculate candidate experience years from employment history
            int totalMonths = 0;
            if (cand.getExperience() != null) {
                LocalDate now = LocalDate.now();
                for (Experience exp : cand.getExperience()) {
                    if (exp.getStartDate() != null) {
                        LocalDate start = exp.getStartDate().toLocalDate();
                        LocalDate end = (exp.getEndDate() != null) ? exp.getEndDate().toLocalDate() : now;
                        if (!end.isBefore(start)) {
                            Period period = Period.between(start, end);
                            totalMonths += (period.getYears() * 12) + period.getMonths();
                        }
                    }
                }
            }
            int years = totalMonths / 12;
            app.setCandidateExperienceYears(years);

            // Format skills
            if (cand.getSkills() != null && !cand.getSkills().isEmpty()) {
                List<String> skillNames = new ArrayList<>();
                for (CandidateSkill cs : cand.getSkills()) {
                    skillNames.add(cs.getSkillName());
                }
                app.setCandidateSkills(String.join(", ", skillNames));
            }

            // Highest Education
            if (cand.getEducation() != null && !cand.getEducation().isEmpty()) {
                Education edu = cand.getEducation().get(0);
                app.setCandidateEducation(edu.getDegree() + " (" + edu.getInstitution() + ")");
            }
        }
    }
}
