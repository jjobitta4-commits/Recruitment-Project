package com.recruitment.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBMigration {

    public static void runMigrations() {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) return;

            DatabaseMetaData meta = conn.getMetaData();

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS email_verifications (" +
                    "  verification_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  email VARCHAR(100) NOT NULL," +
                    "  code VARCHAR(10) NOT NULL," +
                    "  purpose VARCHAR(30) DEFAULT 'REGISTRATION'," +
                    "  expires_at TIMESTAMP NOT NULL," +
                    "  is_used BOOLEAN DEFAULT FALSE," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  INDEX idx_email (email)" +
                    ") ENGINE=InnoDB;"
                );
            }

            boolean hasIsVerified = false;
            try (ResultSet rs = meta.getColumns(null, null, "users", "is_verified")) {
                if (rs.next()) {
                    hasIsVerified = true;
                }
            }

            if (!hasIsVerified) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE;");
                    stmt.executeUpdate("UPDATE users SET is_verified = TRUE;");
                    System.out.println("[DBMigration] Added 'is_verified' column to users table.");
                } catch (Exception ignored) {}
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE users MODIFY COLUMN role ENUM('candidate', 'recruiter', 'admin', 'applicant') NOT NULL;");
            } catch (Exception ignored) {}

            boolean hasLastLogin = false;
            try (ResultSet rs = meta.getColumns(null, null, "users", "last_login")) {
                if (rs.next()) hasLastLogin = true;
            }
            if (!hasLastLogin) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN last_login TIMESTAMP NULL;");
                    System.out.println("[DBMigration] Added 'last_login' column to users table.");
                } catch (Exception ignored) {}
            }

            boolean hasRating = false;
            try (ResultSet rs = meta.getColumns(null, null, "interviews", "rating")) {
                if (rs.next()) hasRating = true;
            }
            if (!hasRating) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE interviews ADD COLUMN rating INT DEFAULT 0;");
                    stmt.executeUpdate("ALTER TABLE interviews ADD COLUMN evaluation_feedback TEXT;");
                    System.out.println("[DBMigration] Added 'rating' and 'evaluation_feedback' to interviews table.");
                } catch (Exception ignored) {}
            }

            boolean hasMatchScore = false;
            try (ResultSet rs = meta.getColumns(null, null, "applications", "match_score")) {
                if (rs.next()) hasMatchScore = true;
            }
            if (!hasMatchScore) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE applications ADD COLUMN match_score INT DEFAULT 0;");
                    System.out.println("[DBMigration] Added 'match_score' column to applications table.");
                } catch (Exception ignored) {}
            }

            try (Statement stmt = conn.createStatement()) {
                int adminCount = 0;
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE email = 'admin@recruithub.com'")) {
                    if (rs.next()) adminCount = rs.getInt(1);
                }
                if (adminCount == 0) {
                    stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, salt, role, status, is_verified) VALUES " +
                        "('admin@recruithub.com', '2069528d0465ab0a75a00a76d1139b34ed357dee6f0fe1b748bd2223ec37e18a', 'kX8f9w1zL2mP4qRt', 'admin', 'active', TRUE);"
                    );
                    System.out.println("[DBMigration] Initialized protected system administrator account (admin@recruithub.com).");
                }
            } catch (Exception ignored) {}

            seedAdaptiveQuestions(conn);
            migrateCareerPaths(conn);

            System.out.println("[DBMigration] Database schema verified successfully.");

        } catch (Exception e) {
            System.err.println("[DBMigration] Notice: " + e.getMessage());
        }
    }

    private static void seedAdaptiveQuestions(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            int count = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM questions")) {
                if (rs.next()) count = rs.getInt(1);
            }
            if (count < 15) {
                // Seed additional questions with clear difficulty levels for adaptive steering
                stmt.executeUpdate(
                    "INSERT INTO questions (assessment_id, skill_id, question_text, option_a, option_b, option_c, option_d, correct_option, difficulty, points) VALUES " +
                    // Easy questions
                    "(1, 1, 'Why is String immutable in Java?', 'To save disk storage only', 'For security, synchronization thread-safety, and String pool caching', 'Because JVM does not support mutable objects', 'To prevent inheritance from Object class', 'B', 'Easy', 10)," +
                    "(1, 4, 'Which SQL constraint uniquely identifies each record in a database table?', 'UNIQUE KEY', 'PRIMARY KEY', 'FOREIGN KEY', 'CHECK', 'B', 'Easy', 10)," +
                    "(2, 7, 'Which HTML5 element represents self-contained content often used for blog posts or news items?', '<section>', '<article>', '<aside>', '<nav>', 'B', 'Easy', 10)," +
                    "(2, 9, 'What is the value of typeof null in JavaScript standard ECMAScript specification?', 'null', 'undefined', 'object', 'boolean', 'C', 'Easy', 10)," +
                    // Medium questions
                    "(1, 1, 'What is the default initial capacity and load factor of a Java standard java.util.HashMap?', '16 and 0.75', '10 and 0.5', '32 and 0.8', '64 and 1.0', 'A', 'Medium', 15)," +
                    "(1, 1, 'In Java 8+ Streams, which of the following is a stateful intermediate operation?', 'filter()', 'map()', 'distinct()', 'peek()', 'C', 'Medium', 15)," +
                    "(2, 9, 'In JavaScript asynchronous event loop, what executes first after the call stack empties?', 'Macro-tasks (setTimeout)', 'Micro-tasks (Promise.then callbacks)', 'requestAnimationFrame callbacks', 'DOM Event listeners', 'B', 'Medium', 15)," +
                    "(2, 4, 'What is the difference between SQL TRUNCATE and DELETE statements?', 'DELETE resets auto-increment; TRUNCATE does not', 'TRUNCATE is DDL and drops/re-creates the table without row-by-row logging; DELETE is DML and removes row-by-row', 'TRUNCATE allows WHERE clauses while DELETE does not', 'TRUNCATE can always be rolled back while DELETE cannot', 'B', 'Medium', 15)," +
                    // Hard questions
                    "(1, 1, 'In Java Concurrency, what is the ABA problem in Lock-Free algorithms, and how is it solved?', 'Deadlock between threads A and B; solved by Lock hierarchy', 'A variable changes from A to B then back to A; solved using AtomicStampedReference', 'A thread yields CPU indefinitely; solved by PriorityQueue', 'Memory leak in ThreadLocal; solved by WeakReference', 'B', 'Hard', 20)," +
                    "(1, 1, 'How does Java 8+ HashMap optimize collision handling when a bucket exceeds TREEIFY_THRESHOLD (8 entries)?', 'It doubles the bucket array instantly', 'It converts the linked list bucket into a Red-Black Tree (TreeNode)', 'It hashes into an overflow secondary table', 'It throws a ConcurrentModificationException', 'B', 'Hard', 20)," +
                    "(2, 3, 'In Cross-Origin Resource Sharing (CORS), which HTTP method is used by browsers for preflight requests?', 'HEAD', 'GET', 'OPTIONS', 'TRACE', 'C', 'Hard', 20)," +
                    "(2, 4, 'How does a B-Tree composite index on (dept_id, salary) behave when a query filters only by WHERE salary > 50000?', 'It performs an optimal index range scan using the entire index', 'It cannot use the composite index efficiently because the leftmost prefix column is missing (Index Skip Scan or Table Scan required)', 'It throws an SQL syntax error', 'It automatically creates a temporary reverse index', 'B', 'Hard', 20);"
                );
                System.out.println("[DBMigration] Seeded comprehensive adaptive question bank across Easy, Medium, and Hard difficulty tiers.");
            }
        } catch (Exception e) {
            System.err.println("[DBMigration.seedAdaptiveQuestions] Notice: " + e.getMessage());
        }
    }

    private static void migrateCareerPaths(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS career_paths (" +
                "  path_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  title VARCHAR(150) NOT NULL UNIQUE," +
                "  category VARCHAR(100) NOT NULL," +
                "  description TEXT," +
                "  required_core_skills VARCHAR(255)," +
                "  min_starting_experience_years INT DEFAULT 0," +
                "  average_market_salary VARCHAR(100)," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_cp_category (category)" +
                ") ENGINE=InnoDB;"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS career_milestones (" +
                "  milestone_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  path_id INT NOT NULL," +
                "  level_order INT NOT NULL," +
                "  level_name VARCHAR(100) NOT NULL," +
                "  experience_years_range VARCHAR(50) NOT NULL," +
                "  salary_range VARCHAR(100) NOT NULL," +
                "  skills_required TEXT NOT NULL," +
                "  milestone_description TEXT," +
                "  recommended_action TEXT," +
                "  INDEX idx_cm_path (path_id)," +
                "  CONSTRAINT fk_cm_path FOREIGN KEY (path_id) REFERENCES career_paths (path_id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;"
            );

            int pathCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM career_paths")) {
                if (rs.next()) pathCount = rs.getInt(1);
            }

            if (pathCount == 0) {
                // Track 1: Cloud & Distributed Backend Systems Architect
                stmt.executeUpdate(
                    "INSERT INTO career_paths (path_id, title, category, description, required_core_skills, min_starting_experience_years, average_market_salary) VALUES " +
                    "(1, 'Cloud & Distributed Backend Systems Architect', 'Backend & Cloud Architecture', " +
                    "'Specializes in high-concurrency enterprise services, resilient database design, containerization, and distributed cloud computing architectures.', " +
                    "'Java, MySQL, JDBC, REST APIs, Docker, Kubernetes, AWS', 0, '$115,000 - $190,000');"
                );

                stmt.executeUpdate(
                    "INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, salary_range, skills_required, milestone_description, recommended_action) VALUES " +
                    "(1, 1, 'Associate Backend Developer (L1)', '0-2 Years', '$75,000 - $95,000', 'Java, MySQL, JDBC, Git', " +
                    "'Build foundational object-oriented services, write clean relational queries, and implement standardized REST endpoints.', " +
                    "'Complete Core Java assessments, implement connection pooling, and publish a relational database CRUD project.'), " +
                    "(1, 2, 'Mid-Level Backend Engineer (L2)', '2-5 Years', '$95,000 - $130,000', 'Java, MySQL, JDBC, REST APIs, Docker', " +
                    "'Design and maintain high-throughput REST APIs, optimize database queries with indexing, and containerize backend microservices.', " +
                    "'Containerize backend services with Docker, optimize slow queries with EXPLAIN, and achieve 80%+ on backend assessments.'), " +
                    "(1, 3, 'Senior Systems Specialist (L3)', '5-8 Years', '$130,000 - $165,000', 'Java, REST APIs, Docker, Kubernetes, AWS', " +
                    "'Lead backend service architecture, design event-driven messaging, and manage production CI/CD container deployments.', " +
                    "'Implement microservice patterns, fault tolerance, and master cloud deployments on AWS or Kubernetes clusters.'), " +
                    "(1, 4, 'Principal Distributed Systems Architect (L4)', '8+ Years', '$165,000 - $220,000+', 'Java, Kubernetes, AWS, System Design, Problem Solving', " +
                    "'Drive organizational technology vision, architect multi-region distributed backends, and mentor engineering leads.', " +
                    "'Architect enterprise fault-tolerant distributed systems, lead system design reviews, and establish architectural RFCs.');"
                );

                // Track 2: Full-Stack Web & Applications Architect
                stmt.executeUpdate(
                    "INSERT INTO career_paths (path_id, title, category, description, required_core_skills, min_starting_experience_years, average_market_salary) VALUES " +
                    "(2, 'Full-Stack Web & Applications Architect', 'Full-Stack Development', " +
                    "'Delivers end-to-end web applications combining modern frontend frameworks, responsive UI standards, scalable server APIs, and relational persistence.', " +
                    "'HTML5, CSS3, JavaScript, React, Java, MySQL, REST APIs', 0, '$105,000 - $175,000');"
                );

                stmt.executeUpdate(
                    "INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, salary_range, skills_required, milestone_description, recommended_action) VALUES " +
                    "(2, 1, 'Junior Web Developer (L1)', '0-2 Years', '$65,000 - $85,000', 'HTML5, CSS3, JavaScript, Git', " +
                    "'Develop responsive web interfaces, handle DOM manipulation, and connect client interfaces to RESTful services.', " +
                    "'Build responsive client-side web applications and master semantic HTML5 & modern CSS3 grid/flexbox layouts.'), " +
                    "(2, 2, 'Full-Stack Developer (L2)', '2-5 Years', '$85,000 - $120,000', 'HTML5, CSS3, JavaScript, Java, MySQL, REST APIs', " +
                    "'Own full-stack features from interactive client UI down to relational database storage and REST APIs.', " +
                    "'Build and deploy end-to-end web applications linking vanilla JS or React with Java REST services.'), " +
                    "(2, 3, 'Senior Full-Stack Engineer (L3)', '5-8 Years', '$120,000 - $155,000', 'JavaScript, React, Java, MySQL, Docker, REST APIs', " +
                    "'Design scalable web application architectures, establish frontend design systems, and ensure web security.', " +
                    "'Optimize Core Web Vitals, implement state management architectures, and secure REST endpoints against OWASP threats.'), " +
                    "(2, 4, 'Principal Web Architect (L4)', '8+ Years', '$155,000 - $210,000+', 'JavaScript, React, Java, System Design, AWS', " +
                    "'Govern enterprise frontend and backend technical standards, design micro-frontends, and mentor engineering teams.', " +
                    "'Author technical standards, direct high-traffic web platform migrations, and optimize full-stack scalability.');"
                );

                // Track 3: Cloud Infrastructure & DevOps Engineer
                stmt.executeUpdate(
                    "INSERT INTO career_paths (path_id, title, category, description, required_core_skills, min_starting_experience_years, average_market_salary) VALUES " +
                    "(3, 'Cloud Infrastructure & DevOps Engineer', 'DevOps & Cloud Infrastructure', " +
                    "'Builds and operates automated CI/CD pipelines, container orchestration clusters, cloud networking, and infrastructure as code.', " +
                    "'Git, Docker, Kubernetes, AWS, Linux, Problem Solving', 0, '$110,000 - $185,000');"
                );

                stmt.executeUpdate(
                    "INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, salary_range, skills_required, milestone_description, recommended_action) VALUES " +
                    "(3, 1, 'Junior Cloud & Systems Engineer (L1)', '0-2 Years', '$70,000 - $90,000', 'Git, Docker, Linux', " +
                    "'Manage basic Linux server administration, write automation scripts, and containerize standalone workloads.', " +
                    "'Acquire Linux CLI fluency and build automated Docker containerization workflows.'), " +
                    "(3, 2, 'DevOps & Automation Specialist (L2)', '2-5 Years', '$90,000 - $125,000', 'Git, Docker, Kubernetes, AWS', " +
                    "'Maintain continuous integration/continuous deployment pipelines and manage cloud container clusters.', " +
                    "'Establish automated CI/CD pipelines and deploy resilient containerized services to Kubernetes clusters.'), " +
                    "(3, 3, 'Senior Cloud Infrastructure Engineer (L3)', '5-8 Years', '$125,000 - $160,000', 'Docker, Kubernetes, AWS, Git, Problem Solving', " +
                    "'Architect multi-cloud infrastructures, implement Infrastructure-as-Code, and enforce security governance.', " +
                    "'Automate cloud provisioning with Infrastructure-as-Code and configure observability dashboards.'), " +
                    "(3, 4, 'Principal Cloud & Platform Architect (L4)', '8+ Years', '$160,000 - $215,000+', 'Kubernetes, AWS, System Design, Problem Solving', " +
                    "'Direct enterprise cloud strategy, design resilient disaster recovery architectures, and lead platform engineering.', " +
                    "'Design zero-downtime multi-region cloud infrastructures and lead SRE organizational excellence.');"
                );

                // Track 4: Data Engineering & Intelligent Systems Specialist
                stmt.executeUpdate(
                    "INSERT INTO career_paths (path_id, title, category, description, required_core_skills, min_starting_experience_years, average_market_salary) VALUES " +
                    "(4, 'Data Engineering & Intelligent Systems Specialist', 'Data & Artificial Intelligence', " +
                    "'Designs scalable big data pipelines, analytical data warehouses, ETL processes, and infrastructure for intelligent machine learning systems.', " +
                    "'Python, SQL, MySQL, PostgreSQL, Problem Solving, Docker', 0, '$115,000 - $185,000');"
                );

                stmt.executeUpdate(
                    "INSERT INTO career_milestones (path_id, level_order, level_name, experience_years_range, salary_range, skills_required, milestone_description, recommended_action) VALUES " +
                    "(4, 1, 'Junior Data Analyst / Data Engineer (L1)', '0-2 Years', '$70,000 - $90,000', 'SQL, MySQL, Python', " +
                    "'Write advanced SQL queries, clean raw datasets, and build basic ETL scripts using Python.', " +
                    "'Master complex SQL queries, window functions, and automate data extraction pipelines in Python.'), " +
                    "(4, 2, 'Data Systems Specialist (L2)', '2-5 Years', '$90,000 - $125,000', 'Python, SQL, MySQL, PostgreSQL, Docker', " +
                    "'Design relational and dimensional data models, optimize data pipelines, and manage database performance.', " +
                    "'Build automated data workflows, implement data warehousing schemas, and optimize ETL execution speeds.'), " +
                    "(4, 3, 'Senior Big Data & Systems Engineer (L3)', '5-8 Years', '$125,000 - $165,000', 'Python, PostgreSQL, Docker, Problem Solving', " +
                    "'Architect distributed streaming and batch data processing platforms, oversee data governance and analytics pipelines.', " +
                    "'Scale distributed streaming pipelines and deploy data ingestion architectures to the cloud.'), " +
                    "(4, 4, 'Principal Data & AI Systems Architect (L4)', '8+ Years', '$165,000 - $225,000+', 'Python, SQL, System Design, Problem Solving', " +
                    "'Define enterprise data architecture, oversee machine learning model serving infrastructure, and steer data strategy.', " +
                    "'Lead enterprise AI data infrastructure roadmaps and architect petabyte-scale real-time data lakes.');"
                );

                System.out.println("[DBMigration] Seeded 4 master career paths with 16 career progression milestones.");
            }
        } catch (Exception e) {
            System.err.println("[DBMigration.migrateCareerPaths] Notice: " + e.getMessage());
        }
    }
}

