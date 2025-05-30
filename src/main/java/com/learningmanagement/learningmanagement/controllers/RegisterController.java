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
import java.util.regex.Pattern;

/**
 * Controller for the registration view, handling user registration with role selection.
 */
public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button registerButton;
    @FXML private Button backToLoginButton;

    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    @FXML
    public void initialize() {
        // Populate roleComboBox
        roleComboBox.getItems().addAll("Admin", "Instructor", "Student");
        roleComboBox.setValue("Student");

        // Add validation listeners
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        roleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());

        // Setup animations
        registerButton.setOnMouseEntered(e -> {
            registerButton.setScaleX(1.05);
            registerButton.setScaleY(1.05);
        });
        registerButton.setOnMouseExited(e -> {
            registerButton.setScaleX(1.0);
            registerButton.setScaleY(1.0);
        });
        backToLoginButton.setOnMouseEntered(e -> {
            backToLoginButton.setScaleX(1.05);
            backToLoginButton.setScaleY(1.05);
        });
        backToLoginButton.setOnMouseExited(e -> {
            backToLoginButton.setScaleX(1.0);
            backToLoginButton.setScaleY(1.0);
        });

        validateForm();
    }

    private void validateForm() {
        boolean isValid = !usernameField.getText().trim().isEmpty() &&
                !passwordField.getText().trim().isEmpty() &&
                passwordField.getText().length() >= 6 &&
                passwordField.getText().equals(confirmPasswordField.getText()) &&
                !fullNameField.getText().trim().isEmpty() &&
                isValidEmail(emailField.getText().trim()) &&
                roleComboBox.getValue() != null;

        registerButton.setDisable(!isValid);

        // Update field styling
        passwordField.setStyle(passwordField.getText().length() >= 6 || passwordField.getText().isEmpty() ?
                "-fx-border-color: #4CAF50;" : "-fx-border-color: #F44336;");
        confirmPasswordField.setStyle(passwordField.getText().equals(confirmPasswordField.getText()) || confirmPasswordField.getText().isEmpty() ?
                "-fx-border-color: #4CAF50;" : "-fx-border-color: #F44336;");
        emailField.setStyle(isValidEmail(emailField.getText().trim()) || emailField.getText().isEmpty() ?
                "-fx-border-color: #4CAF50;" : "-fx-border-color: #F44336;");
    }

    private boolean isValidEmail(String email) {
        return pattern.matcher(email).matches();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();

        if (usernameExists(username)) {
            showAlert("Registration Error", "Username already exists.", Alert.AlertType.ERROR);
            usernameField.requestFocus();
            return;
        }

        if (emailExists(email)) {
            showAlert("Registration Error", "Email already registered.", Alert.AlertType.ERROR);
            emailField.requestFocus();
            return;
        }

        if (registerUser(username, password, fullName, email, role)) {
            showAlert("Registration Successful", "Account created. Please log in.", Alert.AlertType.ERROR);
            navigateToLogin();
        } else {
            showAlert("Registration Error", "Failed to register. Please try again.", Alert.AlertType.ERROR);
        }
    }

    private boolean usernameExists(String username) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean emailExists(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password, String fullName, String email, String role) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String roleSql = "SELECT role_id FROM roles WHERE role_name = ?";
                PreparedStatement roleStmt = conn.prepareStatement(roleSql);
                roleStmt.setString(1, role);
                ResultSet roleRs = roleStmt.executeQuery();
                if (!roleRs.next()) {
                    throw new SQLException("Role not found: " + role);
                }
                int roleId = roleRs.getInt("role_id");

                String userSql = "INSERT INTO users (username, password, role_id, full_name, email) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement userStmt = conn.prepareStatement(userSql);
                userStmt.setString(1, username);
                userStmt.setString(2, password); // Use hashing in production
                userStmt.setInt(3, roleId);
                userStmt.setString(4, fullName);
                userStmt.setString(5, email);

                int affectedRows = userStmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating user failed.");
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/learningmanagement/learningmanagement/login.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("Cannot find login.fxml");
            }
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.setTitle("LMS - Login");
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load login page: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}