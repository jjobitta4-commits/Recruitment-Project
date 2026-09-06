package com.recruitment.service;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.JobDAO;
import com.recruitment.model.*;

import java.util.*;

/**
 * Core Innovation 2: Skill Gap Analyzer & Personalized Learning Path Generator.
 * Identifies exact missing mandatory and preferred skills, detects experience/proficiency
 * shortfalls, and synthesizes an actionable, milestone-driven learning roadmap.
 */
public class SkillGapService {

    private final JobMatchingService matchingService = new JobMatchingService();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final JobDAO jobDAO = new JobDAO();

    /**
     * Performs an exhaustive skill gap audit and builds a personalized learning roadmap.
     */
    public SkillGapReport analyzeSkillGap(Candidate candidate, Job job) {
        SkillGapReport report = new SkillGapReport();
        if (candidate == null || job == null) {
            report.setExecutiveSummary("Candidate or Job profile not found.");
            return report;
        }

        report.setJobId(job.getJobId());
        report.setJobTitle(job.getTitle());
        report.setCompanyName(job.getCompany());
        report.setCandidateId(candidate.getCandidateId());
        report.setCandidateName(candidate.getFullName());

        // 1. Run 5-Factor Match Evaluation
        MatchResult match = matchingService.calculateMatch(candidate, job);
        report.setOverallScore(match.getOverallScore());
        report.setMatchLevel(match.getMatchLevel());
        report.setSkillScore(match.getSkillScore());
        report.setExperienceScore(match.getExperienceScore());
        report.setEducationScore(match.getEducationScore());
        report.setProjectScore(match.getProjectScore());
        report.setAssessmentScore(match.getAssessmentScore());

        // 2. Identify Missing Skills and Proficiency Gaps
        List<JobSkill> jobSkills = job.getJobSkills();
        if (jobSkills == null || jobSkills.isEmpty()) {
            jobSkills = jobDAO.getSkillsForJob(null, job.getJobId());
        }

        List<CandidateSkill> candSkills = candidate.getSkills();
        if (candSkills == null) {
            candSkills = candidateDAO.getSkillsByCandidateId(candidate.getCandidateId());
        }

        List<JobSkill> missingMandatory = new ArrayList<>();
        List<JobSkill> missingPreferred = new ArrayList<>();
        List<JobSkill> acquired = new ArrayList<>();
        List<ProficiencyGap> profGaps = new ArrayList<>();

        for (JobSkill js : jobSkills) {
            CandidateSkill cs = findCandidateSkill(candSkills, js.getSkillName());
            if (cs == null) {
                if (js.isMandatory()) {
                    missingMandatory.add(js);
                } else {
                    missingPreferred.add(js);
                }
            } else {
                acquired.add(js);
                // Check proficiency or years deficiency
                if (js.isMandatory() && cs.getYearsExperience() < js.getMinYearsRequired()) {
                    profGaps.add(new ProficiencyGap(
                            js.getSkillName(),
                            cs.getProficiencyLevel(),
                            cs.getYearsExperience(),
                            js.getMinYearsRequired(),
                            String.format("You have %d yr(s) in %s, but the role requires %d+ yr(s).",
                                    cs.getYearsExperience(), js.getSkillName(), js.getMinYearsRequired())
                    ));
                }
            }
        }

        report.setMissingMandatorySkills(missingMandatory);
        report.setMissingPreferredSkills(missingPreferred);
        report.setAcquiredSkills(acquired);
        report.setProficiencyGaps(profGaps);

        // 3. Synthesize Step-by-Step Personalized Learning Roadmap
        List<RoadmapStep> roadmap = new ArrayList<>();
        int stepCount = 1;
        int totalWeeks = 0;

        // Step Type A: Missing Mandatory Skills (CRITICAL)
        for (JobSkill ms : missingMandatory) {
            RoadmapStep step = generateRoadmapStep(stepCount++, ms.getSkillName(), ms.getCategory(), "CRITICAL", ms.getMinYearsRequired());
            roadmap.add(step);
            totalWeeks += 3;
        }

        // Step Type B: Experience & Proficiency Gaps (RECOMMENDED)
        for (ProficiencyGap pg : profGaps) {
            RoadmapStep step = new RoadmapStep();
            step.setStepNumber(stepCount++);
            step.setSkillName(pg.getSkillName());
            step.setCategory("Core Competency");
            step.setUrgency("RECOMMENDED");
            step.setActionTitle(String.format("Advance %s to Professional Production Standard", pg.getSkillName()));
            step.setActionDescription(String.format("Target: Bridge from %s (%d yrs) to %d+ yrs required depth. Focus on architecture patterns, unit testing, and production tuning.",
                    pg.getCurrentProficiency(), pg.getCurrentYears(), pg.getRequiredYears()));
            step.setEstimatedTime("2-3 Weeks (10 hrs/wk)");
            step.setResources(getCuratedResources(pg.getSkillName()));
            step.setSuggestedProject(String.format("Refactor an existing %s module with clean architecture and automated integration tests.", pg.getSkillName()));
            roadmap.add(step);
            totalWeeks += 2;
        }

        // Step Type C: Missing Preferred Skills (OPTIONAL / BONUS)
        for (JobSkill ps : missingPreferred) {
            RoadmapStep step = generateRoadmapStep(stepCount++, ps.getSkillName(), ps.getCategory(), "OPTIONAL", 1);
            roadmap.add(step);
            totalWeeks += 1;
        }

        report.setLearningRoadmap(roadmap);
        report.setEstimatedWeeksTotal(Math.max(1, totalWeeks));

        // 4. Executive Coaching Summary
        StringBuilder summary = new StringBuilder();
        if (missingMandatory.isEmpty() && profGaps.isEmpty()) {
            summary.append("🎉 Outstanding technical alignment! You meet all mandatory prerequisite requirements. ");
            if (!missingPreferred.isEmpty()) {
                summary.append(String.format("Adding %d optional bonus skill(s) will elevate you into top-percentile candidacy.", missingPreferred.size()));
            } else {
                summary.append("Your skills match 100% of this opening's specification. You are strongly encouraged to apply.");
            }
        } else {
            summary.append(String.format("Skill gap detected: %d mandatory prerequisite(s) missing and %d depth shortfall(s). ",
                    missingMandatory.size(), profGaps.size()));
            summary.append(String.format("Follow the %d-step learning roadmap below (estimated %d weeks total) to bridge these requirements.",
                    roadmap.size(), totalWeeks));
        }
        report.setExecutiveSummary(summary.toString());

        return report;
    }

    public SkillGapReport analyzeSkillGap(int candidateId, int jobId) {
        Candidate c = candidateDAO.getCandidateById(candidateId);
        Job j = jobDAO.getJobById(jobId);
        return analyzeSkillGap(c, j);
    }

    public SkillGapReport analyzeSkillGapForUser(int userId, int jobId) {
        Candidate c = candidateDAO.getCandidateByUserId(userId);
        Job j = jobDAO.getJobById(jobId);
        return analyzeSkillGap(c, j);
    }

    // ==========================================
    // Curated Learning Path & Resources Factory
    // ==========================================

    private RoadmapStep generateRoadmapStep(int stepNum, String skill, String category, String urgency, int reqYears) {
        RoadmapStep step = new RoadmapStep();
        step.setStepNumber(stepNum);
        step.setSkillName(skill);
        step.setCategory(category != null ? category : "Technical");
        step.setUrgency(urgency);

        String title;
        String desc;
        String time;
        String project;

        switch (skill.toLowerCase().trim()) {
            case "java":
                title = "Master Core Java, OOP, Collections & Concurrency";
                desc = "Deep-dive into Java memory management, multithreading (Executors, Locks), Streams API, and JDBC architecture.";
                time = "3-4 Weeks (15 hrs/wk)";
                project = "Build a high-performance multithreaded REST server using Java HttpServer and JDBC.";
                break;
            case "spring boot":
                title = "Build Enterprise REST Microservices with Spring Boot";
                desc = "Learn dependency injection, Spring Data JPA, Spring Security, JWT authentication, and Actuator metrics.";
                time = "3 Weeks (12 hrs/wk)";
                project = "Develop an E-Commerce Order Fulfillment REST API with PostgreSQL.";
                break;
            case "mysql":
            case "postgresql":
            case "database":
                title = "Relational Database Design, Indexing & Query Optimization";
                desc = "Master 3NF schema normalization, ACID transactions, complex joins, composite B-Tree indexes, and EXPLAIN plans.";
                time = "2-3 Weeks (10 hrs/wk)";
                project = "Design and optimize a 15-table relational schema with million-row benchmark testing.";
                break;
            case "rest apis":
                title = "API Design Principles, HTTP Semantics & Security";
                desc = "Master RESTful conventions, status codes, payload serialization, pagination, rate limiting, and CORS headers.";
                time = "1-2 Weeks (10 hrs/wk)";
                project = "Design an OpenAPI 3.0 documented banking transaction API.";
                break;
            case "docker":
                title = "Containerization with Docker & Multi-Stage Builds";
                desc = "Learn Dockerfile best practices, container networking, volume persistence, and multi-service Docker Compose.";
                time = "1-2 Weeks (8 hrs/wk)";
                project = "Containerize a full-stack Java + MySQL application with isolated bridge networks.";
                break;
            case "kubernetes":
                title = "Container Orchestration with Kubernetes (K8s)";
                desc = "Deploy Pods, Deployments, Services, ConfigMaps, Ingress, and auto-scaling rules on local Minikube or cloud.";
                time = "3 Weeks (12 hrs/wk)";
                project = "Deploy a resilient microservice with rolling updates and health probes.";
                break;
            case "aws":
                title = "Cloud Infrastructure with AWS (EC2, S3, RDS, Lambda)";
                desc = "Deploy scalable cloud applications utilizing AWS VPC, IAM policies, RDS MySQL, and serverless Lambda.";
                time = "3 Weeks (10 hrs/wk)";
                project = "Deploy an automated document processing pipeline with S3 triggers and Lambda.";
                break;
            case "javascript":
            case "html5":
            case "css3":
                title = "Modern Responsive Web Development with Vanilla JavaScript";
                desc = "Master ES6+ (Async/Await, Promises), Fetch API, DOM manipulation, Flexbox, and CSS Grid without bloated frameworks.";
                time = "2 Weeks (12 hrs/wk)";
                project = "Build an interactive real-time analytics dashboard with dynamic SVG charting.";
                break;
            case "react":
                title = "Component Architecture & State Management with React";
                desc = "Learn React hooks (useState, useEffect, useReducer), context API, custom hooks, and client-side routing.";
                time = "3 Weeks (12 hrs/wk)";
                project = "Develop a collaborative Kanban board application.";
                break;
            default:
                title = String.format("Acquire Foundational & Practical Skills in %s", skill);
                desc = String.format("Complete hands-on tutorials covering core syntax, architecture patterns, and standard tooling for %s.", skill);
                time = "2-3 Weeks (10 hrs/wk)";
                project = String.format("Develop an end-to-end prototype implementing %s in a production-ready workflow.", skill);
                break;
        }

        step.setActionTitle(title);
        step.setActionDescription(desc);
        step.setEstimatedTime(time);
        step.setResources(getCuratedResources(skill));
        step.setSuggestedProject(project);

        return step;
    }

    private List<Map<String, String>> getCuratedResources(String skill) {
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> r1 = new HashMap<>();
        r1.put("title", skill + " Official Documentation & Reference Manual");
        r1.put("type", "Documentation");
        r1.put("url", "https://dev.java/learn/ or official vendor docs");
        list.add(r1);

        Map<String, String> r2 = new HashMap<>();
        r2.put("title", "FreeCodeCamp / Baeldung Comprehensive Guided Tutorial");
        r2.put("type", "Tutorial");
        r2.put("url", "https://www.baeldung.com / https://www.freecodecamp.org");
        list.add(r2);

        Map<String, String> r3 = new HashMap<>();
        r3.put("title", "GitHub Open-Source Real-World Architecture Examples");
        r3.put("type", "Code Samples");
        r3.put("url", "https://github.com/topics/" + skill.toLowerCase().replaceAll("[^a-z0-9]", ""));
        list.add(r3);

        return list;
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
