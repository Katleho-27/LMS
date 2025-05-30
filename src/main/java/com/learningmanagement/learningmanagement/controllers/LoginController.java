package com.learningmanagement.learningmanagement.controllers;

import com.learningmanagement.learningmanagement.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller for the login view, handling user authentication and navigation.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button showRegisterButton;

    @FXML
    public void initialize() {
        setupValidation();
        setupAnimations();
        usernameField.requestFocus();
    }

    private void setupValidation() {
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        validateForm();
    }

    private void setupAnimations() {
        loginButton.setOnMouseEntered(e -> {
            loginButton.setScaleX(1.05);
            loginButton.setScaleY(1.05);
        });
        loginButton.setOnMouseExited(e -> {
            loginButton.setScaleX(1.0);
            loginButton.setScaleY(1.0);
        });
        showRegisterButton.setOnMouseEntered(e -> {
            showRegisterButton.setScaleX(1.05);
            showRegisterButton.setScaleY(1.05);
        });
        showRegisterButton.setOnMouseExited(e -> {
            showRegisterButton.setScaleX(1.0);
            showRegisterButton.setScaleY(1.0);
        });
    }

    private void validateForm() {
        boolean isValid = !usernameField.getText().trim().isEmpty() &&
                !passwordField.getText().trim().isEmpty();
        loginButton.setDisable(!isValid);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Login Error", "Please enter both username and password.", Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.role_id, r.role_name, u.full_name 
                FROM users u 
                JOIN roles r ON u.role_id = r.role_id 
                WHERE u.username = ? AND u.password = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password); // Use hashing in production
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                int roleId = rs.getInt("role_id");
                String roleName = rs.getString("role_name");
                String fullName = rs.getString("full_name");

                showAlert("Login Successful", "Welcome, " + fullName + "!", Alert.AlertType.INFORMATION);
                navigateToDashboard(userId, roleId, roleName);
            } else {
                showAlert("Login Failed", "Invalid username or password.", Alert.AlertType.ERROR);
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/learningmanagement/learningmanagement/register.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("Cannot find register.fxml");
            }
            Stage stage = (Stage) showRegisterButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.setTitle("LMS - Register");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load registration page: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        showAlert("Info", "Forgot Password functionality is under development.", Alert.AlertType.INFORMATION);
    }

    private void navigateToDashboard(int userId, int roleId, String roleName) {
        try {
            String fxmlPath;
            switch (roleName.toLowerCase()) {
                case "admin":
                    fxmlPath = "/com/learningmanagement/learningmanagement/admin_dashboard.fxml";
                    break;
                case "instructor":
                    fxmlPath = "/com/learningmanagement/learningmanagement/instructor_dashboard.fxml";
                    break;
                case "student":
                    fxmlPath = "/com/learningmanagement/learningmanagement/student_dashboard.fxml";
                    break;
                default:
                    showAlert("Error", "Unknown role: " + roleName, Alert.AlertType.ERROR);
                    return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                showAlert("Navigation Error", "Dashboard file not found: " + fxmlPath, Alert.AlertType.ERROR);
                return;
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 900, 700));
            stage.setTitle("LMS - " + roleName.substring(0, 1).toUpperCase() + roleName.substring(1) + " Dashboard");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}