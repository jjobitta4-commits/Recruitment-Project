-- =========================================================
-- ONLINE RECRUITMENT MANAGEMENT SYSTEM
-- MySQL Database Initialization Script
-- Database Name: recruitment_system
-- =========================================================

-- 1. Create and select Database
CREATE DATABASE IF NOT EXISTS recruitment_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE recruitment_system;

-- 2. Drop existing tables in reverse foreign key order if needed
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS interviews;
DROP TABLE IF EXISTS applications;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS recruiters;
DROP TABLE IF EXISTS applicants;
DROP TABLE IF EXISTS users;

-- =========================================================
-- 3. Table: users
-- Stores user authentication credentials & roles (applicant / recruiter)
-- =========================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('applicant', 'recruiter') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- =========================================================
-- 4. Table: applicants
-- Detailed profile for job seekers
-- =========================================================
CREATE TABLE applicants (
    applicant_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    dob DATE,
    gender VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    education VARCHAR(100),
    university VARCHAR(150),
    graduation_year INT,
    skills TEXT,
    experience_years INT DEFAULT 0,
    expected_salary VARCHAR(50),
    linkedin_url VARCHAR(255),
    github_url VARCHAR(255),
    leetcode_url VARCHAR(255),
    resume_file VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5. Table: recruiters
-- Profile for company recruiters / hiring managers
-- =========================================================
CREATE TABLE recruiters (
    recruiter_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    recruiter_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    company_description TEXT,
    phone VARCHAR(25),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 6. Table: jobs
-- Job openings posted by recruiters
-- =========================================================
CREATE TABLE jobs (
    job_id INT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    company VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    skills_required TEXT NOT NULL,
    education_required VARCHAR(100),
    experience_required VARCHAR(50),
    salary_range VARCHAR(50),
    job_type VARCHAR(50) NOT NULL, -- Full Time, Part Time, Internship, Contract, Remote
    location VARCHAR(100),
    country VARCHAR(100),
    vacancies INT DEFAULT 1,
    deadline DATE,
    status VARCHAR(20) DEFAULT 'Active', -- Active, Closed
    posted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES recruiters(recruiter_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 7. Table: applications
-- Job applications submitted by applicants
-- =========================================================
CREATE TABLE applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    applicant_id INT NOT NULL,
    job_id INT NOT NULL,
    resume_path VARCHAR(255),
    cover_letter TEXT,
    status VARCHAR(30) DEFAULT 'Applied', -- Applied, Under Review, Shortlisted, Interview Scheduled, Selected, Rejected
    applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_applicant_job (applicant_id, job_id),
    FOREIGN KEY (applicant_id) REFERENCES applicants(applicant_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 8. Table: interviews
-- Scheduled interviews for shortlisted applicants
-- =========================================================
CREATE TABLE interviews (
    interview_id INT AUTO_INCREMENT PRIMARY KEY,
    application_id INT NOT NULL,
    applicant_id INT NOT NULL,
    job_id INT NOT NULL,
    interview_date DATE NOT NULL,
    interview_time VARCHAR(20) NOT NULL,
    interview_type VARCHAR(20) DEFAULT 'Online', -- Online, Offline
    meeting_link VARCHAR(255),
    status VARCHAR(30) DEFAULT 'Scheduled', -- Scheduled, Completed, Cancelled
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE,
    FOREIGN KEY (applicant_id) REFERENCES applicants(applicant_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 9. Table: notifications
-- Real-time notifications for both applicants and recruiters
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
-- 10. Sample Data Population
-- Demo accounts:
-- 1. Applicant: applicant@demo.com / pass123
-- 2. Recruiter: recruiter@demo.com / pass123
-- 3. Second Recruiter: hr@techcorp.com / pass123
-- 4. Second Applicant: alex@demo.com / pass123
-- =========================================================

-- Insert Users
INSERT INTO users (user_id, email, password, role) VALUES
(1, 'applicant@demo.com', 'pass123', 'applicant'),
(2, 'recruiter@demo.com', 'pass123', 'recruiter'),
(3, 'hr@techcorp.com', 'pass123', 'recruiter'),
(4, 'alex@demo.com', 'pass123', 'applicant');

-- Insert Applicants
INSERT INTO applicants (applicant_id, user_id, full_name, phone, dob, gender, address, city, country, education, university, graduation_year, skills, experience_years, expected_salary, linkedin_url, github_url, leetcode_url, resume_file) VALUES
(1, 1, 'John Doe', '+1 (555) 234-5678', '1999-05-15', 'Male', '123 Innovation Drive', 'San Francisco', 'United States', 'B.S. in Computer Science', 'Stanford University', 2022, 'Java, Spring Boot, MySQL, REST APIs, JavaScript, HTML5, CSS3, Git, Docker', 2, '$95,000 / yr', 'https://linkedin.com/in/johndoe-sample', 'https://github.com/johndoe-sample', 'https://leetcode.com/johndoe-sample', 'sample_resume_john.pdf'),
(2, 4, 'Alex Smith', '+1 (555) 987-6543', '2001-08-22', 'Female', '456 Tech Park', 'Austin', 'United States', 'B.Tech in Information Technology', 'University of Texas', 2024, 'Python, Django, PostgreSQL, React, JavaScript, AWS', 1, '$80,000 / yr', 'https://linkedin.com/in/alexsmith-demo', 'https://github.com/alexsmith-demo', 'https://leetcode.com/alexsmith-demo', 'sample_resume_alex.pdf');

-- Insert Recruiters
INSERT INTO recruiters (recruiter_id, user_id, recruiter_name, company_name, company_description, phone, country) VALUES
(1, 2, 'Sarah Jenkins', 'Apex Cloud Solutions', 'Apex Cloud Solutions is a next-generation enterprise software and cloud engineering firm helping startups scale reliably.', '+1 (555) 301-4455', 'United States'),
(2, 3, 'David Miller', 'TechCorp Global', 'TechCorp Global provides AI-driven fintech solutions and robust backend infrastructures to Fortune 500 companies.', '+1 (555) 782-9900', 'United States');

-- Insert Jobs
INSERT INTO jobs (job_id, recruiter_id, title, company, description, skills_required, education_required, experience_required, salary_range, job_type, location, country, vacancies, deadline, status) VALUES
(1, 1, 'Junior Java Backend Developer', 'Apex Cloud Solutions', 'We are looking for an energetic Junior Java Developer to join our backend team. You will build high-throughput REST APIs, write clean SQL queries, and collaborate with frontend engineers.', 'Java, JDBC, MySQL, REST APIs, Git', 'B.S. in Computer Science / IT or equivalent', '0 - 2 Years', '$70,000 - $85,000', 'Full Time', 'San Francisco, CA', 'United States', 3, '2026-10-30', 'Active'),
(2, 1, 'Full Stack Web Developer', 'Apex Cloud Solutions', 'Join us to develop modern, responsive client interfaces and robust backend microservices. Strong foundation in JavaScript, HTML5, and relational databases required.', 'HTML5, CSS3, JavaScript, Java, MySQL', 'Bachelor in Engineering / Computer Science', '1 - 3 Years', '$80,000 - $100,000', 'Remote', 'Remote (US/Canada)', 'United States', 2, '2026-11-15', 'Active'),
(3, 1, 'Database Administrator & SQL Specialist', 'Apex Cloud Solutions', 'Seeking a database professional to optimize queries, design schemas, ensure database uptime, and maintain data integrity across production MySQL instances.', 'MySQL, SQL Query Optimization, Database Indexing, Linux', 'Degree in Computer Science or related field', '2 - 4 Years', '$85,000 - $110,000', 'Contract', 'San Jose, CA', 'United States', 1, '2026-09-30', 'Active'),
(4, 2, 'Software Engineering Intern (Java / Web)', 'TechCorp Global', 'Exciting 6-month internship for enthusiastic college students or fresh graduates. Get hands-on mentorship building real-world enterprise web systems in Core Java.', 'Java, OOP, Data Structures, Basic SQL, HTML/CSS', 'Pursuing / Completed Degree in CS or IT', 'Freshers / 0 Years', '$3,500 / month', 'Internship', 'Austin, TX', 'United States', 5, '2026-12-01', 'Active'),
(5, 2, 'Senior Backend Architect', 'TechCorp Global', 'Lead our core services engineering team. Design distributed architectures, guide code reviews, and drive performance optimizations across our cloud platform.', 'Java, Microservices, Distributed Systems, MySQL, Redis', 'M.S. / B.S. in CS', '5+ Years', '$130,000 - $160,000', 'Full Time', 'New York, NY', 'United States', 1, '2026-10-15', 'Active'),
(6, 2, 'Part-Time QA Automation Tester', 'TechCorp Global', 'Flexible part-time role creating automated test suites, validating API endpoints, and executing regression testing on our web services.', 'Java, JUnit, Test Automation, API Testing', 'Associate or Bachelor Degree', '1 - 2 Years', '$35 - $45 / hr', 'Part Time', 'Remote', 'United States', 2, '2026-11-01', 'Active');

-- Insert Sample Applications
INSERT INTO applications (application_id, applicant_id, job_id, resume_path, cover_letter, status, applied_date) VALUES
(1, 1, 1, 'sample_resume_john.pdf', 'I am thrilled to apply for the Junior Java Backend Developer position at Apex Cloud Solutions. With strong skills in Java and MySQL, I look forward to contributing immediately.', 'Shortlisted', '2026-08-25 10:15:00'),
(2, 1, 2, 'sample_resume_john.pdf', 'I have built multiple responsive web applications with vanilla JavaScript and Java backend services. I would love to join your remote team.', 'Under Review', '2026-08-27 14:30:00'),
(3, 2, 4, 'sample_resume_alex.pdf', 'As a recent graduate passionate about Java and software engineering, I would be honored to be selected for the internship at TechCorp Global.', 'Interview Scheduled', '2026-08-28 09:00:00');

-- Insert Sample Interviews
INSERT INTO interviews (interview_id, application_id, applicant_id, job_id, interview_date, interview_time, interview_type, meeting_link, status, notes) VALUES
(1, 1, 1, 1, '2026-09-10', '14:00 EST', 'Online', 'https://meet.google.com/abc-defg-hij', 'Scheduled', 'Technical round covering Core Java, OOP concepts, SQL queries, and a live coding exercise.'),
(2, 3, 2, 4, '2026-09-08', '11:00 EST', 'Online', 'https://meet.google.com/xyz-uvwx-rst', 'Scheduled', 'Initial screening & fundamentals discussion with the hiring manager.');

-- Insert Sample Notifications
INSERT INTO notifications (notification_id, user_id, title, message, is_read) VALUES
(1, 1, 'Application Status Updated', 'Congratulations! Your application for Junior Java Backend Developer at Apex Cloud Solutions has been Shortlisted.', FALSE),
(2, 1, 'Interview Scheduled', 'An online technical interview has been scheduled for Junior Java Backend Developer on Sep 10, 2026 at 14:00 EST.', FALSE),
(3, 2, 'New Application Received', 'John Doe has submitted an application for the Junior Java Backend Developer position.', TRUE),
(4, 2, 'New Application Received', 'John Doe has applied for the Full Stack Web Developer position.', FALSE),
(5, 4, 'Interview Scheduled', 'TechCorp Global has scheduled your interview for Software Engineering Intern on Sep 8, 2026 at 11:00 EST.', FALSE);

-- End of database.sql
