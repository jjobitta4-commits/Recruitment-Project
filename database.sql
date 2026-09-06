-- =========================================================
-- SMART RECRUITMENT AND CANDIDATE SKILL MATCHING SYSTEM
-- Master Normalized MySQL Database Initialization Schema
-- Database: recruitment_system (21 Fully Normalized Tables)
-- =========================================================

CREATE DATABASE IF NOT EXISTS recruitment_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE recruitment_system;

-- Disable FK checks during clean recreation
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS career_milestones;
DROP TABLE IF EXISTS career_paths;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS assessment_answers;
DROP TABLE IF EXISTS assessment_attempts;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS assessments;
DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS job_skills;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS resumes;
DROP TABLE IF EXISTS certifications;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS experience;
DROP TABLE IF EXISTS education;
DROP TABLE IF EXISTS candidate_skills;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS recruiters;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS candidates;
DROP TABLE IF EXISTS email_verifications;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. Table: users
-- Core authentication with SHA-256 password hash + salt & RBAC
-- =========================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role ENUM('candidate', 'recruiter', 'admin') NOT NULL,
    status ENUM('active', 'disabled') DEFAULT 'active',
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- =========================================================
-- 2. Table: email_verifications
-- Automated 6-digit OTP verification codes
-- =========================================================
CREATE TABLE email_verifications (
    verification_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    purpose VARCHAR(30) DEFAULT 'REGISTRATION',
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_verif_email (email)
) ENGINE=InnoDB;

-- =========================================================
-- 3. Table: companies
-- Registered enterprises and hiring organizations
-- =========================================================
CREATE TABLE companies (
    company_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    industry VARCHAR(100),
    website VARCHAR(255),
    location VARCHAR(150),
    approval_status ENUM('pending', 'approved', 'rejected') DEFAULT 'approved',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comp_name (name)
) ENGINE=InnoDB;

-- =========================================================
-- 4. Table: candidates
-- Detailed profiles for job seekers
-- =========================================================
CREATE TABLE candidates (
    candidate_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    dob DATE,
    gender VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    bio TEXT,
    profile_completion INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5. Table: recruiters
-- Company hiring managers & talent acquisition specialists
-- =========================================================
CREATE TABLE recruiters (
    recruiter_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    company_id INT,
    name VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    phone VARCHAR(25),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 6. Table: skills
-- Master taxonomy of technical and soft skills
-- =========================================================
CREATE TABLE skills (
    skill_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    INDEX idx_skill_name (name),
    INDEX idx_skill_cat (category)
) ENGINE=InnoDB;

-- =========================================================
-- 7. Table: candidate_skills
-- Skills declared by candidates with proficiency & years
-- =========================================================
CREATE TABLE candidate_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    skill_id INT NOT NULL,
    proficiency_level ENUM('Beginner', 'Intermediate', 'Advanced', 'Expert') DEFAULT 'Intermediate',
    years_experience INT DEFAULT 1,
    UNIQUE KEY uq_cand_skill (candidate_id, skill_id),
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 8. Table: education
-- Candidate academic history
-- =========================================================
CREATE TABLE education (
    education_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    degree VARCHAR(100) NOT NULL,
    institution VARCHAR(150) NOT NULL,
    field_of_study VARCHAR(100),
    start_year INT,
    end_year INT,
    grade_or_gpa VARCHAR(20),
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 9. Table: experience
-- Candidate employment history
-- =========================================================
CREATE TABLE experience (
    experience_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 10. Table: projects
-- Candidate practical projects & portfolios
-- =========================================================
CREATE TABLE projects (
    project_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    technologies_used VARCHAR(255),
    project_url VARCHAR(255),
    description TEXT,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 11. Table: certifications
-- Candidate professional certifications
-- =========================================================
CREATE TABLE certifications (
    cert_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    certificate_name VARCHAR(150) NOT NULL,
    issuing_org VARCHAR(150) NOT NULL,
    issue_date DATE,
    credential_url VARCHAR(255),
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 12. Table: resumes
-- Uploaded PDF resumes & extracted keyword metadata
-- =========================================================
CREATE TABLE resumes (
    resume_id INT AUTO_INCREMENT PRIMARY KEY,
    candidate_id INT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size INT,
    parsed_skills TEXT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 13. Table: jobs
-- Job openings posted by recruiters
-- =========================================================
CREATE TABLE jobs (
    job_id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    recruiter_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    job_type VARCHAR(50) NOT NULL, -- Full Time, Part Time, Contract, Internship, Remote
    location VARCHAR(100),
    salary_range VARCHAR(50),
    min_experience_years INT DEFAULT 0,
    min_education VARCHAR(100),
    vacancies INT DEFAULT 1,
    deadline DATE,
    approval_status ENUM('pending', 'approved', 'rejected') DEFAULT 'approved',
    status ENUM('Active', 'Closed') DEFAULT 'Active',
    posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_job_status (status),
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE,
    FOREIGN KEY (recruiter_id) REFERENCES recruiters(recruiter_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 14. Table: job_skills
-- Required and preferred skills for a job opening
-- =========================================================
CREATE TABLE job_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    skill_id INT NOT NULL,
    is_mandatory BOOLEAN DEFAULT TRUE,
    min_years_required INT DEFAULT 1,
    UNIQUE KEY uq_job_skill (job_id, skill_id),
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 15. Table: applications
-- Job applications with AI match scores & hiring stages
-- =========================================================
CREATE TABLE applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    candidate_id INT NOT NULL,
    resume_id INT,
    cover_letter TEXT,
    match_score INT DEFAULT 0,
    status ENUM('Applied', 'Under_Review', 'Shortlisted', 'Interview_Scheduled', 'Selected', 'Rejected') DEFAULT 'Applied',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_job_candidate (job_id, candidate_id),
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 16. Table: assessments
-- Online test suites associated with jobs/skills
-- =========================================================
CREATE TABLE assessments (
    assessment_id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    passing_score INT DEFAULT 60,
    time_limit_minutes INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 17. Table: questions
-- Multi-difficulty questions for adaptive testing
-- =========================================================
CREATE TABLE questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    assessment_id INT NOT NULL,
    skill_id INT,
    question_text TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_option CHAR(1) NOT NULL, -- 'A', 'B', 'C', or 'D'
    difficulty ENUM('Easy', 'Medium', 'Hard') NOT NULL,
    points INT DEFAULT 10,
    FOREIGN KEY (assessment_id) REFERENCES assessments(assessment_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 18. Table: assessment_attempts
-- Candidate adaptive test submissions & performance
-- =========================================================
CREATE TABLE assessment_attempts (
    attempt_id INT AUTO_INCREMENT PRIMARY KEY,
    assessment_id INT NOT NULL,
    candidate_id INT NOT NULL,
    total_score INT DEFAULT 0,
    max_score INT DEFAULT 100,
    difficulty_reached ENUM('Easy', 'Medium', 'Hard') DEFAULT 'Easy',
    is_passed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assessment_id) REFERENCES assessments(assessment_id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 19. Table: assessment_answers
-- Individual question answers in an assessment attempt
-- =========================================================
CREATE TABLE assessment_answers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    attempt_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_option CHAR(1) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    points_awarded INT DEFAULT 0,
    FOREIGN KEY (attempt_id) REFERENCES assessment_attempts(attempt_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 20. Table: interviews
-- Scheduled interviews with comprehensive multi-factor scoring
-- =========================================================
CREATE TABLE interviews (
    interview_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL,
    recruiter_id INT NOT NULL,
    candidate_id INT NOT NULL,
    interview_date DATE NOT NULL,
    interview_time VARCHAR(20) NOT NULL,
    interview_type VARCHAR(20) DEFAULT 'Online', -- Online, Offline
    meeting_link VARCHAR(255),
    status ENUM('Scheduled', 'Completed', 'Cancelled') DEFAULT 'Scheduled',
    technical_score INT DEFAULT 0,
    communication_score INT DEFAULT 0,
    problem_solving_score INT DEFAULT 0,
    overall_score INT DEFAULT 0,
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
    FOREIGN KEY (recruiter_id) REFERENCES recruiters(recruiter_id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidates(candidate_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 21. Table: notifications
-- Real-time notification alerts for all user roles
-- =========================================================
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 22. Table: audit_logs
-- System security, admin actions & compliance audit trail
-- =========================================================
CREATE TABLE audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id INT,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- SEED DATA POPULATION
-- =========================================================

-- 1. Master Skills Taxonomy
INSERT INTO skills (skill_id, name, category) VALUES
(1, 'Java', 'Backend'),
(2, 'Spring Boot', 'Backend'),
(3, 'REST APIs', 'Backend'),
(4, 'MySQL', 'Database'),
(5, 'JDBC', 'Database'),
(6, 'PostgreSQL', 'Database'),
(7, 'HTML5', 'Frontend'),
(8, 'CSS3', 'Frontend'),
(9, 'JavaScript', 'Frontend'),
(10, 'TypeScript', 'Frontend'),
(11, 'React', 'Frontend'),
(12, 'Docker', 'DevOps'),
(13, 'Kubernetes', 'DevOps'),
(14, 'AWS', 'Cloud'),
(15, 'Git', 'Tools'),
(16, 'Python', 'Backend'),
(17, 'Django', 'Backend'),
(18, 'Data Structures', 'Fundamentals'),
(19, 'Algorithms', 'Fundamentals'),
(20, 'System Design', 'Architecture'),
(21, 'Problem Solving', 'Soft Skills'),
(22, 'Communication', 'Soft Skills'),
(23, 'Microservices', 'Architecture'),
(24, 'JUnit', 'Testing');

-- 2. Master Companies
INSERT INTO companies (company_id, name, description, industry, website, location, approval_status) VALUES
(1, 'Apex Cloud Solutions', 'Next-generation cloud infrastructure and enterprise backend systems.', 'Information Technology', 'https://apexcloud.com', 'San Francisco, CA', 'approved'),
(2, 'TechCorp Global', 'FinTech and AI-powered financial solutions for Fortune 500 enterprises.', 'Financial Technology', 'https://techcorpglobal.com', 'New York, NY', 'approved'),
(3, 'InnovateLabs Inc', 'Cutting-edge AI and machine learning development agency.', 'Artificial Intelligence', 'https://innovatelabs.io', 'Austin, TX', 'approved');

-- 3. Initial Users (Protected System Administrator Account)
-- Default Password: admin@recruithub.com -> admin123
INSERT INTO users (user_id, email, password_hash, salt, role, status, is_verified) VALUES
(1, 'admin@recruithub.com', '2069528d0465ab0a75a00a76d1139b34ed357dee6f0fe1b748bd2223ec37e18a', 'kX8f9w1zL2mP4qRt', 'admin', 'active', TRUE);

-- 4. Assessments & Adaptive Questions Catalog
INSERT INTO assessments (assessment_id, job_id, title, description, passing_score, time_limit_minutes) VALUES
(1, NULL, 'Core Java & Backend Architecture Assessment', 'Evaluates object-oriented programming, concurrency, JDBC transactions, and REST design.', 65, 20),
(2, NULL, 'Java Full Stack Technical Screening', 'Tests Core Java fundamentals, SQL queries, and basic web standards.', 60, 15);

-- Adaptive Questions (Easy, Medium, Hard)
INSERT INTO questions (question_id, assessment_id, skill_id, question_text, option_a, option_b, option_c, option_d, correct_option, difficulty, points) VALUES
-- Easy
(1, 1, 1, 'Which Java collection class guarantees that elements are maintained in insertion order?', 'HashSet', 'LinkedHashSet', 'TreeSet', 'Vector', 'B', 'Easy', 10),
(2, 1, 4, 'Which SQL clause is used to filter aggregated grouped records?', 'WHERE', 'HAVING', 'GROUP BY', 'ORDER BY', 'B', 'Easy', 10),
-- Medium
(3, 1, 1, 'In Java, how does the try-with-resources statement manage resource closing?', 'Requires explicit close() in finally', 'Closes objects that implement AutoCloseable in reverse order of declaration', 'Relies solely on Garbage Collection', 'Closes objects asynchronously via daemon thread', 'B', 'Medium', 15),
(4, 1, 5, 'Why are PreparedStatements preferred over Statement in JDBC?', 'They compile faster only', 'They prevent SQL injection and support query pre-compilation with placeholders', 'They eliminate network round-trips', 'They do not require connection closure', 'B', 'Medium', 15),
-- Hard
(5, 1, 1, 'In Java Concurrency, what is the key difference between volatile and synchronized?', 'volatile provides atomicity only', 'volatile ensures visibility without mutual exclusion locks, whereas synchronized ensures both visibility and mutual exclusion', 'synchronized only works on primitive types', 'volatile creates an internal reentrant lock', 'B', 'Hard', 20),
(6, 1, 4, 'Which MySQL transaction isolation level prevents both Dirty Reads and Non-Repeatable Reads but permits Phantom Reads in standard SQL?', 'READ UNCOMMITTED', 'READ COMMITTED', 'REPEATABLE READ', 'SERIALIZABLE', 'C', 'Hard', 20);

-- 16. Audit Log Seed
INSERT INTO audit_logs (user_id, action, entity_name, entity_id, details, ip_address) VALUES
(1, 'SYSTEM_INITIALIZATION', 'SYSTEM', 1, 'Initial 21-table normalized schema created with seed master skills and demo accounts.', '127.0.0.1');

-- =========================================================
-- 17. Table: career_paths
-- Core Innovation 5: Career Path Trajectory Tracks
-- =========================================================
CREATE TABLE career_paths (
    path_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    required_core_skills VARCHAR(255),
    min_starting_experience_years INT DEFAULT 0,
    average_market_salary VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cp_category (category)
) ENGINE=InnoDB;

-- =========================================================
-- 18. Table: career_milestones
-- 4-Stage Progressive Milestone Ladders per Career Track
-- =========================================================
CREATE TABLE career_milestones (
    milestone_id INT AUTO_INCREMENT PRIMARY KEY,
    path_id INT NOT NULL,
    level_order INT NOT NULL,
    level_name VARCHAR(100) NOT NULL,
    experience_years_range VARCHAR(50) NOT NULL,
    salary_range VARCHAR(100) NOT NULL,
    skills_required TEXT NOT NULL,
    milestone_description TEXT,
    recommended_action TEXT,
    INDEX idx_cm_path (path_id),
    CONSTRAINT fk_cm_path FOREIGN KEY (path_id) REFERENCES career_paths (path_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Seed Career Paths
INSERT INTO career_paths (path_id, title, category, description, required_core_skills, min_starting_experience_years, average_market_salary) VALUES
(1, 'Cloud & Distributed Backend Systems Architect', 'Backend & Cloud Architecture', 'Specializes in high-concurrency enterprise services, resilient database design, containerization, and distributed cloud computing architectures.', 'Java, MySQL, JDBC, REST APIs, Docker, Kubernetes, AWS', 0, '$115,000 - $190,000'),
(2, 'Full-Stack Web & Applications Architect', 'Full-Stack Development', 'Delivers end-to-end web applications combining modern frontend frameworks, responsive UI standards, scalable server APIs, and relational persistence.', 'HTML5, CSS3, JavaScript, React, Java, MySQL, REST APIs', 0, '$105,000 - $175,000'),
(3, 'Cloud Infrastructure & DevOps Engineer', 'DevOps & Cloud Infrastructure', 'Builds and operates automated CI/CD pipelines, container orchestration clusters, cloud networking, and infrastructure as code.', 'Git, Docker, Kubernetes, AWS, Linux, Problem Solving', 0, '$110,000 - $185,000'),
(4, 'Data Engineering & Intelligent Systems Specialist', 'Data & Artificial Intelligence', 'Designs scalable big data pipelines, analytical data warehouses, ETL processes, and infrastructure for intelligent machine learning systems.', 'Python, SQL, MySQL, PostgreSQL, Problem Solving, Docker', 0, '$115,000 - $185,000');

-- Seed Career Milestones
INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, salary_range, skills_required, milestone_description, recommended_action) VALUES
(1, 1, 'Associate Backend Developer (L1)', '0-2 Years', '$75,000 - $95,000', 'Java, MySQL, JDBC, Git', 'Build foundational object-oriented services, write clean relational queries, and implement standardized REST endpoints.', 'Complete Core Java assessments, implement connection pooling, and publish a relational database CRUD project.'),
(1, 2, 'Mid-Level Backend Engineer (L2)', '2-5 Years', '$95,000 - $130,000', 'Java, MySQL, JDBC, REST APIs, Docker', 'Design and maintain high-throughput REST APIs, optimize database queries with indexing, and containerize backend microservices.', 'Containerize backend services with Docker, optimize slow queries with EXPLAIN, and achieve 80%+ on backend assessments.'),
(1, 3, 'Senior Systems Specialist (L3)', '5-8 Years', '$130,000 - $165,000', 'Java, REST APIs, Docker, Kubernetes, AWS', 'Lead backend service architecture, design event-driven messaging, and manage production CI/CD container deployments.', 'Implement microservice patterns, fault tolerance, and master cloud deployments on AWS or Kubernetes clusters.'),
(1, 4, 'Principal Distributed Systems Architect (L4)', '8+ Years', '$165,000 - $220,000+', 'Java, Kubernetes, AWS, System Design, Problem Solving', 'Drive organizational technology vision, architect multi-region distributed backends, and mentor engineering leads.', 'Architect enterprise fault-tolerant distributed systems, lead system design reviews, and establish architectural RFCs.'),

(2, 1, 'Junior Web Developer (L1)', '0-2 Years', '$65,000 - $85,000', 'HTML5, CSS3, JavaScript, Git', 'Develop responsive web interfaces, handle DOM manipulation, and connect client interfaces to RESTful services.', 'Build responsive client-side web applications and master semantic HTML5 & modern CSS3 grid/flexbox layouts.'),
(2, 2, 'Full-Stack Developer (L2)', '2-5 Years', '$85,000 - $120,000', 'HTML5, CSS3, JavaScript, Java, MySQL, REST APIs', 'Own full-stack features from interactive client UI down to relational database storage and REST APIs.', 'Build and deploy end-to-end web applications linking vanilla JS or React with Java REST services.'),
(2, 3, 'Senior Full-Stack Engineer (L3)', '5-8 Years', '$120,000 - $155,000', 'JavaScript, React, Java, MySQL, Docker, REST APIs', 'Design scalable web application architectures, establish frontend design systems, and ensure web security.', 'Optimize Core Web Vitals, implement state management architectures, and secure REST endpoints against OWASP threats.'),
(2, 4, 'Principal Web Architect (L4)', '8+ Years', '$155,000 - $210,000+', 'JavaScript, React, Java, System Design, AWS', 'Govern enterprise frontend and backend technical standards, design micro-frontends, and mentor engineering teams.', 'Author technical standards, direct high-traffic web platform migrations, and optimize full-stack scalability.'),

(3, 1, 'Junior Cloud & Systems Engineer (L1)', '0-2 Years', '$70,000 - $90,000', 'Git, Docker, Linux', 'Manage basic Linux server administration, write automation scripts, and containerize standalone workloads.', 'Acquire Linux CLI fluency and build automated Docker containerization workflows.'),
(3, 2, 'DevOps & Automation Specialist (L2)', '2-5 Years', '$90,000 - $125,000', 'Git, Docker, Kubernetes, AWS', 'Maintain continuous integration/continuous deployment pipelines and manage cloud container clusters.', 'Establish automated CI/CD pipelines and deploy resilient containerized services to Kubernetes clusters.'),
(3, 3, 'Senior Cloud Infrastructure Engineer (L3)', '5-8 Years', '$125,000 - $160,000', 'Docker, Kubernetes, AWS, Git, Problem Solving', 'Architect multi-cloud infrastructures, implement Infrastructure-as-Code, and enforce security governance.', 'Automate cloud provisioning with Infrastructure-as-Code and configure observability dashboards.'),
(3, 4, 'Principal Cloud & Platform Architect (L4)', '8+ Years', '$160,000 - $215,000+', 'Kubernetes, AWS, System Design, Problem Solving', 'Direct enterprise cloud strategy, design resilient disaster recovery architectures, and lead platform engineering.', 'Design zero-downtime multi-region cloud infrastructures and lead SRE organizational excellence.'),

(4, 1, 'Junior Data Analyst / Data Engineer (L1)', '0-2 Years', '$70,000 - $90,000', 'SQL, MySQL, Python', 'Write advanced SQL queries, clean raw datasets, and build basic ETL scripts using Python.', 'Master complex SQL queries, window functions, and automate data extraction pipelines in Python.'),
(4, 2, 'Data Systems Specialist (L2)', '2-5 Years', '$90,000 - $125,000', 'Python, SQL, MySQL, PostgreSQL, Docker', 'Design relational and dimensional data models, optimize data pipelines, and manage database performance.', 'Build automated data workflows, implement data warehousing schemas, and optimize ETL execution speeds.'),
(4, 3, 'Senior Big Data & Systems Engineer (L3)', '5-8 Years', '$125,000 - $165,000', 'Python, PostgreSQL, Docker, Problem Solving', 'Architect distributed streaming and batch data processing platforms, oversee data governance and analytics pipelines.', 'Scale distributed streaming pipelines and deploy data ingestion architectures to the cloud.'),
(4, 4, 'Principal Data & AI Systems Architect (L4)', '8+ Years', '$165,000 - $225,000+', 'Python, SQL, System Design, Problem Solving', 'Define enterprise data architecture, oversee machine learning model serving infrastructure, and steer data strategy.', 'Lead enterprise AI data infrastructure roadmaps and architect petabyte-scale real-time data lakes.');

-- Done
