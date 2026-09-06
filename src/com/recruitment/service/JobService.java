package com.recruitment.service;

import com.recruitment.dao.CandidateDAO;
import com.recruitment.dao.CompanyDAO;
import com.recruitment.dao.JobDAO;
import com.recruitment.dao.RecruiterDAO;
import com.recruitment.model.Company;
import com.recruitment.model.Job;
import com.recruitment.model.JobSkill;
import com.recruitment.model.Recruiter;
import com.recruitment.model.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service Layer for Job Vacancies, Dual Skill Requirements, and Admin Approval Workflows.
 */
public class JobService {

    private final JobDAO jobDAO = new JobDAO();
    private final CompanyDAO companyDAO = new CompanyDAO();
    private final RecruiterDAO recruiterDAO = new RecruiterDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();

    /**
     * Creates a new job posting with mandatory and preferred skills.
     */
    public boolean createJobPosting(Job job, List<Map<String, Object>> skillInputs, int recruiterId, String companyName) {
        if (job == null || recruiterId <= 0) {
            return false;
        }

        job.setRecruiterId(recruiterId);

        // 1. Resolve Company
        if (job.getCompanyId() <= 0) {
            Recruiter r = recruiterDAO.getRecruiterById(recruiterId);
            String compName = (companyName != null && !companyName.trim().isEmpty())
                    ? companyName.trim()
                    : (r != null && r.getCompanyName() != null ? r.getCompanyName() : "Tech Innovations Inc");

            Company comp = companyDAO.getOrCreateCompany(compName, "Technology", job.getLocation());
            if (comp != null) {
                job.setCompanyId(comp.getCompanyId());
                job.setCompany(comp.getName());
                if (r != null && r.getCompanyId() <= 0) {
                    recruiterDAO.setRecruiterCompany(recruiterId, comp.getCompanyId());
                }
            } else {
                job.setCompanyId(1); // Default fallback to first seed company
            }
        }

        // 2. Resolve Skills into List<JobSkill>
        List<JobSkill> jobSkills = new ArrayList<>();

        if (skillInputs != null && !skillInputs.isEmpty()) {
            for (Map<String, Object> input : skillInputs) {
                String name = input.containsKey("name") ? String.valueOf(input.get("name")).trim() : "";
                if (name.isEmpty()) continue;

                String category = input.containsKey("category") ? String.valueOf(input.get("category")).trim() : "Technical";
                boolean isMandatory = true;
                if (input.containsKey("isMandatory")) {
                    Object val = input.get("isMandatory");
                    if (val instanceof Boolean) isMandatory = (Boolean) val;
                    else if (val instanceof String) isMandatory = Boolean.parseBoolean((String) val);
                }

                int minYears = 1;
                if (input.containsKey("minYearsRequired")) {
                    try {
                        minYears = Integer.parseInt(String.valueOf(input.get("minYearsRequired")));
                    } catch (NumberFormatException ignored) {}
                }

                Skill masterSkill = candidateDAO.getOrCreateSkill(name, category);
                if (masterSkill != null) {
                    JobSkill js = new JobSkill();
                    js.setSkillId(masterSkill.getSkillId());
                    js.setSkillName(masterSkill.getName());
                    js.setCategory(masterSkill.getCategory());
                    js.setMandatory(isMandatory);
                    js.setMinYearsRequired(Math.max(1, minYears));
                    jobSkills.add(js);
                }
            }
        } else if (job.getSkillsRequired() != null && !job.getSkillsRequired().trim().isEmpty()) {
            // Fallback comma-separated parsing
            String[] parts = job.getSkillsRequired().split("[,;|]");
            for (String p : parts) {
                String sName = p.trim();
                if (!sName.isEmpty()) {
                    Skill masterSkill = candidateDAO.getOrCreateSkill(sName, "Technical");
                    if (masterSkill != null) {
                        jobSkills.add(new JobSkill(masterSkill.getSkillId(), masterSkill.getName(), masterSkill.getCategory(), true, Math.max(1, job.getMinExperienceYears())));
                    }
                }
            }
        }

        return jobDAO.createJob(job, jobSkills);
    }

    /**
     * Updates an existing job posting and synchronizes its skills.
     */
    public boolean updateJobPosting(Job job, List<Map<String, Object>> skillInputs, int recruiterId) {
        if (job == null || job.getJobId() <= 0) return false;

        Job existing = jobDAO.getJobById(job.getJobId());
        if (existing == null || existing.getRecruiterId() != recruiterId) {
            return false;
        }

        List<JobSkill> jobSkills = new ArrayList<>();
        if (skillInputs != null && !skillInputs.isEmpty()) {
            for (Map<String, Object> input : skillInputs) {
                String name = input.containsKey("name") ? String.valueOf(input.get("name")).trim() : "";
                if (name.isEmpty()) continue;

                String category = input.containsKey("category") ? String.valueOf(input.get("category")).trim() : "Technical";
                boolean isMandatory = true;
                if (input.containsKey("isMandatory")) {
                    Object val = input.get("isMandatory");
                    if (val instanceof Boolean) isMandatory = (Boolean) val;
                    else if (val instanceof String) isMandatory = Boolean.parseBoolean((String) val);
                }

                int minYears = 1;
                if (input.containsKey("minYearsRequired")) {
                    try {
                        minYears = Integer.parseInt(String.valueOf(input.get("minYearsRequired")));
                    } catch (NumberFormatException ignored) {}
                }

                Skill masterSkill = candidateDAO.getOrCreateSkill(name, category);
                if (masterSkill != null) {
                    JobSkill js = new JobSkill();
                    js.setJobId(job.getJobId());
                    js.setSkillId(masterSkill.getSkillId());
                    js.setSkillName(masterSkill.getName());
                    js.setCategory(masterSkill.getCategory());
                    js.setMandatory(isMandatory);
                    js.setMinYearsRequired(Math.max(1, minYears));
                    jobSkills.add(js);
                }
            }
        }

        return jobDAO.updateJob(job, jobSkills);
    }

    public Job getJobById(int jobId) {
        return jobDAO.getJobById(jobId);
    }

    public List<Job> getAllActiveJobs() {
        return jobDAO.getAllActiveJobs();
    }

    public List<Job> getJobsByRecruiter(int recruiterId) {
        return jobDAO.getJobsByRecruiter(recruiterId);
    }

    public boolean updateJobStatus(int jobId, String status, int recruiterId) {
        Job existing = jobDAO.getJobById(jobId);
        if (existing == null || existing.getRecruiterId() != recruiterId) {
            return false;
        }
        return jobDAO.updateJobStatus(jobId, status);
    }

    public boolean deleteJob(int jobId, int recruiterId) {
        Job existing = jobDAO.getJobById(jobId);
        if (existing == null || existing.getRecruiterId() != recruiterId) {
            return false;
        }
        return jobDAO.deleteJob(jobId);
    }

    public List<Job> searchJobs(String keyword, String jobType, String country, String location, String skill, String experience) {
        return jobDAO.searchJobs(keyword, jobType, country, location, skill, experience);
    }

    // ==========================================
    // Administrator Moderation Workflows
    // ==========================================
    public List<Job> getPendingJobsForAdmin() {
        return jobDAO.getPendingJobs();
    }

    public List<Job> getAllJobsForAdmin(String filterApproval) {
        return jobDAO.getAllJobsForAdmin(filterApproval);
    }

    public boolean approveJob(int jobId) {
        return jobDAO.updateJobApprovalStatus(jobId, "approved");
    }

    public boolean rejectJob(int jobId) {
        return jobDAO.updateJobApprovalStatus(jobId, "rejected");
    }
}
