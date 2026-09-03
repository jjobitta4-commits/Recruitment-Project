# 🎓 Online Recruitment Management System
### Core Java + JDBC + Java Built-in `HttpServer` + Vanilla HTML5/CSS3/JavaScript

---

## 📌 Project Overview

The **Online Recruitment Management System** is a full-stack web application developed without external frameworks (**no Spring Boot, no Servlets, no Tomcat, no Hibernate, no Node.js, no React/Bootstrap**). 

The backend runs entirely on standard **Java SE (Core Java)** utilizing Java's built-in `com.sun.net.httpserver.HttpServer` and **JDBC with `PreparedStatement`**, while the frontend is built using standard **HTML5, modern CSS3, and Vanilla JavaScript (`fetch` API)**.

This project is specifically structured for **college mini-projects, viva demonstrations, and junior Java developers** looking to master backend fundamentals and multi-tier architectural patterns.

---

## 🏗️ System Architecture

```text
[ Browser / Frontend Client ]
        │  (HTTP / REST-like JSON / Multipart Form-Data)
        ▼
[ Java Built-in HttpServer (Port 8080) ]
        │
   ┌────┴───────────────────────────┐
   │ Static File Handler            │  (Serves HTML, CSS, JS, Resume PDFs)
   │ API Handlers                   │  (Login, Register, Jobs, Applications, etc.)
   └────┬───────────────────────────┘
        │
[ DAO Layer (PreparedStatement) ]
        │
[ JDBC Driver (MySQL Connector/J) ]
        │
[ MySQL Database: recruitment_system ]
```

---

## 📁 Directory Structure

```text
recruitmentproject/
│
├── src/
│   └── com/
│       └── recruitment/
│           ├── Main.java                   # Main entry point (starts server)
│           │
│           ├── server/
│           │   ├── Server.java             # HttpServer configuration & routing
│           │   ├── StaticFileHandler.java  # Serves frontend files & resume PDFs
│           │   ├── LoginHandler.java       # Login, logout & session checks
│           │   ├── RegisterHandler.java    # Applicant & Recruiter signup
│           │   ├── JobHandler.java         # Job CRUD & search/filtering
│           │   ├── ApplicationHandler.java # Job applications & review statuses
│           │   ├── RecruiterHandler.java   # Recruiter stats & candidate review
│           │   ├── ApplicantHandler.java   # Applicant profile & resume upload
│           │   ├── InterviewHandler.java   # Interview scheduling & tracking
│           │   └── NotificationHandler.java# In-app notifications
│           │
│           ├── dao/
│           │   ├── UserDAO.java            # User credentials & authentication
│           │   ├── ApplicantDAO.java       # Job seeker profiles & resumes
│           │   ├── RecruiterDAO.java       # Recruiter profiles & metrics
│           │   ├── JobDAO.java             # Job listings, filters & queries
│           │   ├── ApplicationDAO.java     # Applications & status workflows
│           │   ├── InterviewDAO.java       # Interview records & scheduling
│           │   └── NotificationDAO.java    # User notification records
│           │
│           ├── model/
│           │   ├── User.java               # User entity
│           │   ├── Applicant.java          # Applicant profile entity
│           │   ├── Recruiter.java          # Recruiter profile entity
│           │   ├── Job.java                # Job vacancy entity
│           │   ├── Application.java        # Application submission entity
│           │   ├── Interview.java          # Scheduled interview entity
│           │   └── Notification.java       # Notification message entity
│           │
│           └── util/
│               ├── DBConnection.java       # Centralized JDBC connection provider
│               ├── SessionManager.java     # In-memory session tracking
│               ├── JSONUtil.java           # Zero-dependency JSON parser & serializer
│               ├── ResponseHelper.java     # HTTP response & CORS utility
│               └── MultipartParser.java    # Pure Java multipart/form-data parser
│
├── frontend/
│   ├── index.html                          # Landing page & featured jobs
│   ├── login.html                          # Login screen with 1-click demo buttons
│   ├── register.html                       # Applicant & Recruiter registration
│   ├── jobs.html                           # Public job board with live filters
│   │
│   ├── applicant/
│   │   ├── dashboard.html                  # Overview metrics & recent activity
│   │   ├── profile.html                    # Profile editor & PDF resume manager
│   │   ├── applications.html               # Application status tracker
│   │   ├── interviews.html                 # Scheduled interview list & meeting links
│   │   └── notifications.html              # Applicant alerts center
│   │
│   ├── recruiter/
│   │   ├── dashboard.html                  # Recruiter hiring overview & statistics
│   │   ├── post-job.html                   # Create new vacancy posting form
│   │   ├── manage-jobs.html                # Edit, delete & toggle active/closed jobs
│   │   ├── candidates.html                 # Screening, resume viewing & status update
│   │   ├── interviews.html                 # Manage scheduled interview rounds
│   │   └── notifications.html              # Recruiter alerts center
│   │
│   ├── css/
│   │   └── style.css                       # Modern, responsive stylesheet
│   │
│   └── js/
│       ├── common.js                       # Auth helpers, modals, navbar & toast alerts
│       ├── login.js                        # Login logic & demo autofill
│       ├── register.js                     # Multi-tab signup & file upload
│       ├── jobs.js                         # Public search, filters & apply modal
│       ├── applicant.js                    # Applicant dashboard & profile scripts
│       └── recruiter.js                    # Recruiter dashboard, jobs & candidate scripts
│
├── uploads/
│   └── resumes/                            # Stored applicant PDF resumes
│       ├── sample_resume_john.pdf
│       └── sample_resume_alex.pdf
│
├── lib/
│   └── mysql-connector-j.jar               # Official MySQL JDBC Driver
│
├── database.sql                            # Complete MySQL database schema & sample data
└── README.md                               # Project documentation & setup instructions
```

---

## 👥 Demo User Accounts (Pre-configured)

| Role | Email | Password | Details |
| :--- | :--- | :--- | :--- |
| **Applicant** | `applicant@demo.com` | `pass123` | John Doe (2 Yrs Exp, Java Backend) |
| **Applicant** | `alex@demo.com` | `pass123` | Alex Smith (1 Yr Exp, Full Stack) |
| **Recruiter** | `recruiter@demo.com` | `pass123` | Sarah Jenkins (Apex Cloud Solutions) |
| **Recruiter** | `hr@techcorp.com` | `pass123` | David Miller (TechCorp Global) |

> 💡 *On the login page (`/login.html`), you can click any of the **1-Click Demo Buttons** to autofill these credentials instantly!*

---

## ⚙️ Prerequisites

1. **Java Development Kit (JDK 17 or higher)** (JDK 17, 21, or 25).
2. **MySQL Server** (Community Edition or Workbench).
3. Any code editor: **VS Code** or **IntelliJ IDEA**.

---

## 🗄️ Database Setup (MySQL)

1. Open **MySQL Workbench** or MySQL CLI.
2. Open and execute the provided file:
   ```sql
   database.sql
   ```
3. This creates the database `recruitment_system`, creates all 7 relational tables, and inserts sample users, applicants, recruiters, jobs, applications, interviews, and notifications.

### Verifying Database Credentials
Open [`src/com/recruitment/util/DBConnection.java`](file:///c:/Users/jjobi/OneDrive/Documents/recruitmentproject/src/com/recruitment/util/DBConnection.java) and verify your MySQL root password:
```java
private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/recruitment_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String DEFAULT_USER = "root";
private static final String DEFAULT_PASSWORD = "root"; // Change to your local MySQL password (or "" if empty)
```

---

## 🚀 How to Run the Project

### Option A: Using Visual Studio Code (Recommended)
1. Open the project folder in **VS Code**.
2. Make sure you have the official **Extension Pack for Java** installed.
3. Open [`src/com/recruitment/Main.java`](file:///c:/Users/jjobi/OneDrive/Documents/recruitmentproject/src/com/recruitment/Main.java).
4. Click **Run** or **Debug** (or press `F5`).
5. Open your browser and navigate to:
   ```text
   http://localhost:8080
   ```

---

### Option B: Using IntelliJ IDEA
1. Open **IntelliJ IDEA** &rarr; **File** &rarr; **Open...** &rarr; Select the `recruitmentproject` directory.
2. Go to **File** &rarr; **Project Structure** &rarr; **Libraries** &rarr; Click **+** &rarr; Select `lib/mysql-connector-j.jar`.
3. Open [`src/com/recruitment/Main.java`](file:///c:/Users/jjobi/OneDrive/Documents/recruitmentproject/src/com/recruitment/Main.java).
4. Click the green **Run** arrow next to `public static void main(String[] args)`.
5. Open your browser and navigate to:
   ```text
   http://localhost:8080
   ```

---

### Option C: Using Command Line (Terminal / PowerShell)

#### On Windows (PowerShell / Command Prompt):
```powershell
# 1. Compile all Java source files into the bin/ directory
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar;src" -d bin src/com/recruitment/Main.java src/com/recruitment/model/*.java src/com/recruitment/dao/*.java src/com/recruitment/util/*.java src/com/recruitment/server/*.java

# 2. Run the application
java -cp "bin;lib/*" com.recruitment.Main
```

#### On Linux / macOS:
```bash
# 1. Compile
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar:src" -d bin src/com/recruitment/Main.java src/com/recruitment/model/*.java src/com/recruitment/dao/*.java src/com/recruitment/util/*.java src/com/recruitment/server/*.java

# 2. Run
java -cp "bin:lib/*" com.recruitment.Main
```

---

## 🌐 Application Console Output

When the server starts successfully, the following output appears in your terminal:

```text
=====================================
   ONLINE RECRUITMENT SYSTEM
=====================================

Server started successfully.

Open:
http://localhost:8080

[Database] Connected successfully to MySQL ('recruitment_system').
=====================================
Press Ctrl+C in this terminal to stop the server.
```

---

## 📡 REST-like API Endpoints

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/login` | Authenticates user & returns session token | No |
| `POST` | `/api/logout` | Invalidates active user session | Yes |
| `POST` | `/api/register` | Registers Applicant (with PDF resume) or Recruiter | No |
| `GET` | `/api/jobs` | Lists active jobs or searches by keyword/filter | No |
| `POST` | `/api/jobs` | Posts a new job vacancy | Yes (Recruiter) |
| `PUT` | `/api/jobs` | Updates an existing job vacancy | Yes (Recruiter) |
| `DELETE`| `/api/jobs` | Deletes a job posting | Yes (Recruiter) |
| `PUT` | `/api/jobs/status` | Toggles status between `Active` and `Closed` | Yes (Recruiter) |
| `GET` | `/api/jobs/recruiter`| Lists jobs posted by logged-in recruiter | Yes (Recruiter) |
| `POST` | `/api/applications`| Submits a new job application (with resume) | Yes (Applicant) |
| `GET` | `/api/applications`| Gets submitted applications or received applicants | Yes |
| `PUT` | `/api/applications/status`| Updates status (Shortlisted, Selected, Rejected) | Yes (Recruiter) |
| `GET` | `/api/applicant/profile` | Retrieves applicant's profile | Yes (Applicant) |
| `PUT` | `/api/applicant/profile` | Updates applicant's profile | Yes (Applicant) |
| `POST` | `/api/applicant/resume` | Uploads/updates PDF resume file | Yes (Applicant) |
| `GET` | `/api/applicant/stats` | Gets applicant dashboard metrics | Yes (Applicant) |
| `GET` | `/api/recruiter/stats` | Gets recruiter hiring metrics | Yes (Recruiter) |
| `GET` | `/api/recruiter/candidates` | Lists all candidate profiles for screening | Yes (Recruiter) |
| `GET` | `/api/interviews` | Lists scheduled interviews | Yes |
| `POST` | `/api/interviews` | Schedules a new interview round | Yes (Recruiter) |
| `PUT` | `/api/interviews/status` | Updates interview status (Scheduled/Completed/Cancelled) | Yes (Recruiter) |
| `GET` | `/api/notifications` | Gets user notifications & unread count | Yes |
| `PUT` | `/api/notifications/read` | Marks notifications as read | Yes |

---

## 🔒 Security & Best Practices Implemented

1. **SQL Injection Prevention**: All SQL queries strictly use `PreparedStatement` with parameterized values (`?`).
2. **Input Validation**: Server-side checks for email formats, passwords, required fields, and valid date formats.
3. **File Upload Security**:
   - Only `.pdf` files accepted.
   - File size validation (< 10MB).
   - Sanitized filenames generated with unique UUIDs (`resume_<uuid>_<safeName>.pdf`) to avoid overwriting or directory traversal.
4. **Thread-Safe In-Memory Sessions**: Token-based session tracking with concurrent maps and role checks.
5. **No Duplicate Applications**: Database unique constraint and DAO checks prevent duplicate submissions for the same opening.

---

## 🏆 Viva / Evaluation Questions & Answers

**Q1: How does this project serve web pages without Tomcat or Spring Boot?**
> **A**: It uses Java SE's built-in `com.sun.net.httpserver.HttpServer`. The `Server` class binds to port 8080 and maps HTTP contexts (routes) to custom handlers (`LoginHandler`, `StaticFileHandler`, etc.).

**Q2: How does the DAO pattern work in this project?**
> **A**: Data Access Objects (`UserDAO`, `JobDAO`, `ApplicationDAO`, etc.) isolate all database operations. HTTP handlers never contain direct SQL queries, ensuring modularity and maintainability.

**Q3: How are PDF resumes uploaded without third-party libraries like Apache Commons FileUpload?**
> **A**: We wrote a custom pure Core Java `MultipartParser.java` that scans multipart boundary markers, parses `Content-Disposition` headers, and streams the binary PDF bytes directly to disk in `uploads/resumes/`.

---

## 📄 License
This project is developed for educational and college mini-project purposes. You are free to modify and expand it!
