package com.recruitment.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC Database Connection Provider.
 * Connects to the MySQL database 'recruitment_system'.
 * 
 * Note: You can customize DB_USER and DB_PASSWORD to match your local MySQL configuration,
 * or specify them via system properties/environment variables:
 * -Ddb.url=... -Ddb.user=... -Ddb.password=...
 */
public class DBConnection {
    // Default connection parameters
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/recruitment_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "jobijohn765"; // Common default, change if your root has another password or empty ""

    private static final String URL = System.getProperty("db.url", 
            System.getenv("DB_URL") != null ? System.getenv("DB_URL") : DEFAULT_URL);
    private static final String USER = System.getProperty("db.user", 
            System.getenv("DB_USER") != null ? System.getenv("DB_USER") : DEFAULT_USER);
    private static final String PASSWORD = System.getProperty("db.password", 
            System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : DEFAULT_PASSWORD);

    static {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("--------------------------------------------------");
            System.err.println("[ERROR] MySQL JDBC Driver not found in classpath!");
            System.err.println("Make sure mysql-connector-j.jar is in the lib/ directory.");
            System.err.println("--------------------------------------------------");
            e.printStackTrace();
        }
    }

    /**
     * Obtains a new JDBC Connection to MySQL.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Failed to connect to MySQL database.");
            System.err.println("[DBConnection] URL: " + URL + " | User: " + USER);
            System.err.println("[DBConnection] Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if the database is reachable.
     * @return true if connection succeeds, false otherwise.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
