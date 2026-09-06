package com.recruitment.dao;

import com.recruitment.model.*;
import com.recruitment.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Data Access Object for Adaptive Assessments, Questions, Attempts, and Answers.
 * Exclusively utilizes pure JDBC with PreparedStatements.
 */
public class AssessmentDAO {

    public List<Assessment> getAllAssessments() {
        List<Assessment> list = new ArrayList<>();
        String sql = "SELECT a.*, j.title AS job_title, comp.name AS company_name, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id) AS total_questions, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Easy') AS easy_count, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Medium') AS medium_count, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Hard') AS hard_count " +
                     "FROM assessments a " +
                     "LEFT JOIN jobs j ON a.job_id = j.job_id " +
                     "LEFT JOIN companies comp ON j.company_id = comp.company_id " +
                     "ORDER BY a.assessment_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapAssessment(rs));
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getAllAssessments] Error: " + e.getMessage());
        }
        return list;
    }

    public Assessment getAssessmentById(int assessmentId) {
        String sql = "SELECT a.*, j.title AS job_title, comp.name AS company_name, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id) AS total_questions, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Easy') AS easy_count, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Medium') AS medium_count, " +
                     "  (SELECT COUNT(*) FROM questions q WHERE q.assessment_id = a.assessment_id AND q.difficulty = 'Hard') AS hard_count " +
                     "FROM assessments a " +
                     "LEFT JOIN jobs j ON a.job_id = j.job_id " +
                     "LEFT JOIN companies comp ON j.company_id = comp.company_id " +
                     "WHERE a.assessment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAssessment(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getAssessmentById] Error: " + e.getMessage());
        }
        return null;
    }

    public boolean createAssessment(Assessment assessment) {
        String sql = "INSERT INTO assessments (job_id, title, description, passing_score, time_limit_minutes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (assessment.getJobId() != null && assessment.getJobId() > 0) {
                ps.setInt(1, assessment.getJobId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, assessment.getTitle());
            ps.setString(3, assessment.getDescription());
            ps.setInt(4, assessment.getPassingScore() > 0 ? assessment.getPassingScore() : 60);
            ps.setInt(5, assessment.getTimeLimitMinutes() > 0 ? assessment.getTimeLimitMinutes() : 20);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        assessment.setAssessmentId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.createAssessment] Error: " + e.getMessage());
        }
        return false;
    }

    public List<Question> getQuestionsByAssessment(int assessmentId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS skill_name FROM questions q " +
                     "LEFT JOIN skills s ON q.skill_id = s.skill_id " +
                     "WHERE q.assessment_id = ? ORDER BY q.difficulty DESC, q.question_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuestion(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getQuestionsByAssessment] Error: " + e.getMessage());
        }
        return list;
    }

    public List<Question> getQuestionsByDifficulty(int assessmentId, String difficulty) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.*, s.name AS skill_name FROM questions q " +
                     "LEFT JOIN skills s ON q.skill_id = s.skill_id " +
                     "WHERE q.assessment_id = ? AND q.difficulty = ? ORDER BY q.question_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, assessmentId);
            ps.setString(2, difficulty);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuestion(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getQuestionsByDifficulty] Error: " + e.getMessage());
        }
        return list;
    }

    public Question getQuestionById(int questionId) {
        String sql = "SELECT q.*, s.name AS skill_name FROM questions q " +
                     "LEFT JOIN skills s ON q.skill_id = s.skill_id " +
                     "WHERE q.question_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapQuestion(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getQuestionById] Error: " + e.getMessage());
        }
        return null;
    }

    public boolean createQuestion(Question q) {
        String sql = "INSERT INTO questions (assessment_id, skill_id, question_text, option_a, option_b, option_c, option_d, correct_option, difficulty, points) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, q.getAssessmentId());
            if (q.getSkillId() != null && q.getSkillId() > 0) {
                ps.setInt(2, q.getSkillId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, q.getQuestionText());
            ps.setString(4, q.getOptionA());
            ps.setString(5, q.getOptionB());
            ps.setString(6, q.getOptionC());
            ps.setString(7, q.getOptionD());
            ps.setString(8, q.getCorrectOption());
            ps.setString(9, q.getDifficulty());
            ps.setInt(10, q.getPoints() > 0 ? q.getPoints() : 10);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        q.setQuestionId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.createQuestion] Error: " + e.getMessage());
        }
        return false;
    }

    public int createAttempt(AssessmentAttempt attempt) {
        String sql = "INSERT INTO assessment_attempts (assessment_id, candidate_id, total_score, max_score, difficulty_reached, is_passed) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, attempt.getAssessmentId());
            ps.setInt(2, attempt.getCandidateId());
            ps.setInt(3, attempt.getTotalScore());
            ps.setInt(4, attempt.getMaxScore() > 0 ? attempt.getMaxScore() : 100);
            ps.setString(5, attempt.getDifficultyReached() != null ? attempt.getDifficultyReached() : "Easy");
            ps.setBoolean(6, attempt.isPassed());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        attempt.setAttemptId(id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.createAttempt] Error: " + e.getMessage());
        }
        return -1;
    }

    public AssessmentAttempt getAttemptById(int attemptId) {
        String sql = "SELECT aa.*, a.title AS assessment_title, a.passing_score, c.full_name AS candidate_name, " +
                     "  (SELECT COUNT(*) FROM assessment_answers ans WHERE ans.attempt_id = aa.attempt_id) AS total_answers " +
                     "FROM assessment_attempts aa " +
                     "JOIN assessments a ON aa.assessment_id = a.assessment_id " +
                     "JOIN candidates c ON aa.candidate_id = c.candidate_id " +
                     "WHERE aa.attempt_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAttempt(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getAttemptById] Error: " + e.getMessage());
        }
        return null;
    }

    public List<AssessmentAttempt> getAttemptsByCandidate(int candidateId) {
        List<AssessmentAttempt> list = new ArrayList<>();
        String sql = "SELECT aa.*, a.title AS assessment_title, a.passing_score, c.full_name AS candidate_name, " +
                     "  (SELECT COUNT(*) FROM assessment_answers ans WHERE ans.attempt_id = aa.attempt_id) AS total_answers " +
                     "FROM assessment_attempts aa " +
                     "JOIN assessments a ON aa.assessment_id = a.assessment_id " +
                     "JOIN candidates c ON aa.candidate_id = c.candidate_id " +
                     "WHERE aa.candidate_id = ? " +
                     "ORDER BY aa.completed_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttempt(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getAttemptsByCandidate] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean recordAnswer(AssessmentAnswer answer) {
        String sql = "INSERT INTO assessment_answers (attempt_id, question_id, selected_option, is_correct, points_awarded) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, answer.getAttemptId());
            ps.setInt(2, answer.getQuestionId());
            ps.setString(3, answer.getSelectedOption());
            ps.setBoolean(4, answer.isCorrect());
            ps.setInt(5, answer.getPointsAwarded());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        answer.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.recordAnswer] Error: " + e.getMessage());
        }
        return false;
    }

    public List<AssessmentAnswer> getAnswersByAttempt(int attemptId) {
        List<AssessmentAnswer> list = new ArrayList<>();
        String sql = "SELECT ans.*, q.question_text, q.correct_option, q.difficulty FROM assessment_answers ans " +
                     "JOIN questions q ON ans.question_id = q.question_id " +
                     "WHERE ans.attempt_id = ? ORDER BY ans.id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AssessmentAnswer a = new AssessmentAnswer();
                    a.setId(rs.getInt("id"));
                    a.setAttemptId(rs.getInt("attempt_id"));
                    a.setQuestionId(rs.getInt("question_id"));
                    a.setSelectedOption(rs.getString("selected_option"));
                    a.setCorrect(rs.getBoolean("is_correct"));
                    a.setPointsAwarded(rs.getInt("points_awarded"));
                    a.setQuestionText(rs.getString("question_text"));
                    a.setCorrectOption(rs.getString("correct_option"));
                    a.setDifficulty(rs.getString("difficulty"));
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.getAnswersByAttempt] Error: " + e.getMessage());
        }
        return list;
    }

    public boolean finalizeAttempt(int attemptId, int totalScore, int maxScore, String difficultyReached, boolean isPassed) {
        String sql = "UPDATE assessment_attempts SET total_score = ?, max_score = ?, difficulty_reached = ?, is_passed = ?, completed_at = CURRENT_TIMESTAMP WHERE attempt_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, totalScore);
            ps.setInt(2, maxScore);
            ps.setString(3, difficultyReached);
            ps.setBoolean(4, isPassed);
            ps.setInt(5, attemptId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[AssessmentDAO.finalizeAttempt] Error: " + e.getMessage());
        }
        return false;
    }

    private Assessment mapAssessment(ResultSet rs) throws SQLException {
        Assessment a = new Assessment();
        a.setAssessmentId(rs.getInt("assessment_id"));
        int jId = rs.getInt("job_id");
        if (!rs.wasNull()) a.setJobId(jId);
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setPassingScore(rs.getInt("passing_score"));
        a.setTimeLimitMinutes(rs.getInt("time_limit_minutes"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setJobTitle(rs.getString("job_title"));
        a.setCompanyName(rs.getString("company_name"));
        a.setTotalQuestions(rs.getInt("total_questions"));
        a.setEasyQuestionsCount(rs.getInt("easy_count"));
        a.setMediumQuestionsCount(rs.getInt("medium_count"));
        a.setHardQuestionsCount(rs.getInt("hard_count"));
        return a;
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setQuestionId(rs.getInt("question_id"));
        q.setAssessmentId(rs.getInt("assessment_id"));
        int sId = rs.getInt("skill_id");
        if (!rs.wasNull()) q.setSkillId(sId);
        q.setSkillName(rs.getString("skill_name"));
        q.setQuestionText(rs.getString("question_text"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setOptionC(rs.getString("option_c"));
        q.setOptionD(rs.getString("option_d"));
        q.setCorrectOption(rs.getString("correct_option"));
        q.setDifficulty(rs.getString("difficulty"));
        q.setPoints(rs.getInt("points"));
        return q;
    }

    private AssessmentAttempt mapAttempt(ResultSet rs) throws SQLException {
        AssessmentAttempt at = new AssessmentAttempt();
        at.setAttemptId(rs.getInt("attempt_id"));
        at.setAssessmentId(rs.getInt("assessment_id"));
        at.setCandidateId(rs.getInt("candidate_id"));
        at.setTotalScore(rs.getInt("total_score"));
        at.setMaxScore(rs.getInt("max_score"));
        at.setDifficultyReached(rs.getString("difficulty_reached"));
        at.setPassed(rs.getBoolean("is_passed"));
        at.setCompletedAt(rs.getTimestamp("completed_at"));
        at.setAssessmentTitle(rs.getString("assessment_title"));
        at.setCandidateName(rs.getString("candidate_name"));
        at.setPassingScore(rs.getInt("passing_score"));
        at.setTotalQuestionsAnswered(rs.getInt("total_answers"));
        return at;
    }
}
