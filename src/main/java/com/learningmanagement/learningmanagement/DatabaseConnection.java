package com.learningmanagement.learningmanagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/lmsdb";
    private static final String USER = "postgres"; // Update with your PostgreSQL username
    private static final String PASSWORD = "kash"; // Update with your PostgreSQL password

    public static Connection getConnection() throws SQLException {
        try {
            // Check if the driver class is available
            Class<?> driverClass = Class.forName("org.postgresql.Driver");
            if (driverClass == null) {
                throw new ClassNotFoundException("PostgreSQL JDBC Driver class is null.");
            }
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found. Ensure the driver JAR is in the classpath or module path.", e);
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}