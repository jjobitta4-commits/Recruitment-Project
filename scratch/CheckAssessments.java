import java.sql.*;

public class CheckAssessments {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/recruitment_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "jobijohn765")) {
            try (Statement s = c.createStatement()) {
                try (ResultSet rs = s.executeQuery("SELECT assessment_id, title, passing_score FROM assessments")) {
                    while (rs.next()) {
                        System.out.println("Assessment: " + rs.getInt("assessment_id") + " - " + rs.getString("title") + " (Passing: " + rs.getInt("passing_score") + "%)");
                    }
                }
                try (ResultSet rs = s.executeQuery("SELECT count(*), difficulty FROM questions GROUP BY difficulty")) {
                    while (rs.next()) {
                        System.out.println("Questions [" + rs.getString("difficulty") + "]: " + rs.getInt(1));
                    }
                }
            }
        }
    }
}
