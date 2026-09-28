package com.eventhub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection manager for the `eventhub` MySQL database.
 *
 * Credentials can be overridden with environment variables so the same jar
 * runs locally and on a server:
 *   EVENTHUB_DB_URL, EVENTHUB_DB_USER, EVENTHUB_DB_PASSWORD
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/eventhub"
          + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String URL      = envOr("EVENTHUB_DB_URL", DEFAULT_URL);
    private static final String USER     = envOr("EVENTHUB_DB_USER", "root");
    private static final String PASSWORD = envOr("EVENTHUB_DB_PASSWORD", "root");

    static {
        try {
            // Explicit load keeps older containers happy; JDBC 4 auto-loads too.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "MySQL JDBC driver not found on the classpath: " + e.getMessage());
        }
    }

    private DatabaseConnection() { /* utility class */ }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    /** Opens a new connection. Always use inside try-with-resources. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Quick health check used by EventHubApp on start-up. */
    public static boolean testConnection() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
            return false;
        }
    }
}
