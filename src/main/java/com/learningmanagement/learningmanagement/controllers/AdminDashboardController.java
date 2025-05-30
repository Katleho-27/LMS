package com.learningmanagement.learningmanagement.controllers;

import com.learningmanagement.learningmanagement.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class AdminDashboardController {

    // Main UI Components
    @FXML private BorderPane rootPane;
    @FXML private StackPane contentStackPane;
    @FXML private VBox dashboardPane;
    @FXML private VBox userManagementPane;
    @FXML private VBox courseManagementPane;

    // Dashboard components
    @FXML private Label currentUserLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label totalEnrollmentsLabel;
    @FXML private ProgressIndicator systemHealthIndicator;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label systemHealthLabel;
    @FXML private Label overallProgressLabel;

    // User Management Components
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Number> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private TextField userSearchField;

    // Course Management Components
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, Number> courseIdColumn;
    @FXML private TableColumn<Course, String> courseNameColumn;
    @FXML private TableColumn<Course, String> instructorColumn;
    @FXML private TableColumn<Course, Number> enrolledCountColumn;
    @FXML private TextField courseSearchField;

    // Button references
    @FXML private Button logoutButton;
    @FXML private Button createUserButton;
    @FXML private Button updateUserButton;
    @FXML private Button deleteUserButton;
    @FXML private Button resetPasswordButton;
    @FXML private Button createCourseButton;
    @FXML private Button updateCourseButton;
    @FXML private Button deleteCourseButton;
    @FXML private Button assignInstructorButton;
    @FXML private Button manageEnrollmentsButton;

    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();

    public void initialize() {
        setupTableColumns();
        loadInitialData();
        setupEventHandlers();
        addButtonEffects();
        showDashboard();
        updateQuickStats();

        // Initial fade in
        FadeTransition fade = new FadeTransition(Duration.millis(800), contentStackPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void setupTableColumns() {
        // User table columns
        userIdColumn.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());

        // Course table columns
        courseIdColumn.setCellValueFactory(cellData -> cellData.getValue().courseIdProperty());
        courseNameColumn.setCellValueFactory(cellData -> cellData.getValue().courseNameProperty());
        instructorColumn.setCellValueFactory(cellData -> cellData.getValue().instructorProperty());
        enrolledCountColumn.setCellValueFactory(cellData -> cellData.getValue().enrolledCountProperty());

        // Setup role filter combo
        roleFilterCombo.setItems(FXCollections.observableArrayList("All", "Admin", "Instructor", "Student"));
        roleFilterCombo.setValue("All");
    }

    private void loadInitialData() {
        loadUsers();
        loadCourses();
        updateSystemHealth();
    }

    private void loadUsers() {
        allUsers.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username, r.role_name
                FROM users u 
                JOIN roles r ON u.role_id = r.role_id 
                ORDER BY u.user_id
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allUsers.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role_name")
                ));
            }
            userTable.setItems(allUsers);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
        }
    }

    private void loadCourses() {
        allCourses.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT c.course_id, c.course_name, u.username as instructor_name,
                       COUNT(e.user_id) as enrolled_count
                FROM courses c
                LEFT JOIN users u ON c.instructor_id = u.user_id
                LEFT JOIN enrollments e ON c.course_id = e.course_id
                GROUP BY c.course_id, c.course_name, u.username
                ORDER BY c.course_id
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allCourses.add(new Course(
                        rs.getInt("course_id"),
                        rs.getString("course_name"),
                        rs.getString("instructor_name") != null ? rs.getString("instructor_name") : "Unassigned",
                        rs.getInt("enrolled_count")
                ));
            }
            courseTable.setItems(allCourses);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    // ==================== USER MANAGEMENT FUNCTIONS ====================

    @FXML
    private void createUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Create New User");
        dialog.setHeaderText("Enter user details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> role = new ComboBox<>();
        role.setItems(FXCollections.observableArrayList("Admin", "Instructor", "Student"));
        role.setValue("Student");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(role, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // Get role_id
                    String roleQuery = "SELECT role_id FROM roles WHERE role_name = ?";
                    PreparedStatement roleStmt = conn.prepareStatement(roleQuery);
                    roleStmt.setString(1, role.getValue());
                    ResultSet roleRs = roleStmt.executeQuery();

                    if (roleRs.next()) {
                        int roleId = roleRs.getInt("role_id");

                        String sql = "INSERT INTO users (username, password, role_id) VALUES (?, ?, ?)";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, username.getText());
                        stmt.setString(2, password.getText()); // In production, hash this password
                        stmt.setInt(3, roleId);

                        stmt.executeUpdate();
                        loadUsers();
                        updateQuickStats();
                        showAlert("Success", "User created successfully!");
                    }
                } catch (SQLException e) {
                    showAlert("Error", "Failed to create user: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void updateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to update.");
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Update User");
        dialog.setHeaderText("Update user details for: " + selectedUser.getUsername());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField(selectedUser.getUsername());
        ComboBox<String> role = new ComboBox<>();
        role.setItems(FXCollections.observableArrayList("Admin", "Instructor", "Student"));
        role.setValue(selectedUser.getRole());

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(role, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // Get role_id
                    String roleQuery = "SELECT role_id FROM roles WHERE role_name = ?";
                    PreparedStatement roleStmt = conn.prepareStatement(roleQuery);
                    roleStmt.setString(1, role.getValue());
                    ResultSet roleRs = roleStmt.executeQuery();

                    if (roleRs.next()) {
                        int roleId = roleRs.getInt("role_id");

                        String sql = "UPDATE users SET username = ?, role_id = ? WHERE user_id = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, username.getText());
                        stmt.setInt(2, roleId);
                        stmt.setInt(3, selectedUser.getUserId());

                        stmt.executeUpdate();
                        loadUsers();
                        loadCourses(); // Refresh courses in case instructor was updated
                        showAlert("Success", "User updated successfully!");
                    }
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update user: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void deleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete user: " + selectedUser.getUsername() + "?\nThis action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First delete related records
                String deleteProgress = "DELETE FROM progress WHERE user_id = ?";
                PreparedStatement progressStmt = conn.prepareStatement(deleteProgress);
                progressStmt.setInt(1, selectedUser.getUserId());
                progressStmt.executeUpdate();

                String deleteEnrollments = "DELETE FROM enrollments WHERE user_id = ?";
                PreparedStatement enrollStmt = conn.prepareStatement(deleteEnrollments);
                enrollStmt.setInt(1, selectedUser.getUserId());
                enrollStmt.executeUpdate();

                // Then delete the user
                String sql = "DELETE FROM users WHERE user_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedUser.getUserId());
                stmt.executeUpdate();

                loadUsers();
                loadCourses(); // Refresh courses
                updateQuickStats();
                showAlert("Success", "User deleted successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete user: " + e.getMessage());
            }
        }
    }

    @FXML
    private void resetPassword() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Error", "Please select a user to reset password.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("Reset password for: " + selectedUser.getUsername());
        dialog.setContentText("Enter new password:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE users SET password = ? WHERE user_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, result.get()); // In production, hash this password
                stmt.setInt(2, selectedUser.getUserId());
                stmt.executeUpdate();

                showAlert("Success", "Password reset successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to reset password: " + e.getMessage());
            }
        }
    }

    // ==================== COURSE MANAGEMENT FUNCTIONS ====================

    @FXML
    private void createCourse() {
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Create New Course");
        dialog.setHeaderText("Enter course details");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField courseName = new TextField();
        ComboBox<String> instructor = new ComboBox<>();

        // Load instructors
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username 
                FROM users u JOIN roles r ON u.role_id = r.role_id 
                WHERE r.role_name = 'Instructor'
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> instructors = FXCollections.observableArrayList();
            while (rs.next()) {
                instructors.add(rs.getInt("user_id") + " - " + rs.getString("username"));
            }
            instructor.setItems(instructors);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load instructors: " + e.getMessage());
        }

        grid.add(new Label("Course Name:"), 0, 0);
        grid.add(courseName, 1, 0);
        grid.add(new Label("Instructor:"), 0, 1);
        grid.add(instructor, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    int instructorId = Integer.parseInt(instructor.getValue().split(" - ")[0]);

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "INSERT INTO courses (course_name, instructor_id) VALUES (?, ?)";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, courseName.getText());
                        stmt.setInt(2, instructorId);

                        stmt.executeUpdate();
                        loadCourses();
                        updateQuickStats();
                        showAlert("Success", "Course created successfully!");
                    }
                } catch (SQLException | NumberFormatException e) {
                    showAlert("Error", "Failed to create course: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void updateCourse() {
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showAlert("Error", "Please select a course to update.");
            return;
        }

        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle("Update Course");
        dialog.setHeaderText("Update course: " + selectedCourse.getCourseName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField courseName = new TextField(selectedCourse.getCourseName());

        grid.add(new Label("Course Name:"), 0, 0);
        grid.add(courseName, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE courses SET course_name = ? WHERE course_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, courseName.getText());
                    stmt.setInt(2, selectedCourse.getCourseId());

                    stmt.executeUpdate();
                    loadCourses();
                    showAlert("Success", "Course updated successfully!");
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update course: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void deleteCourse() {
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showAlert("Error", "Please select a course to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Course");
        confirmAlert.setContentText("Are you sure you want to delete course: " + selectedCourse.getCourseName() + "?\nThis will also delete all enrollments and progress for this course.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First delete related records
                String deleteProgress = "DELETE FROM progress WHERE course_id = ?";
                PreparedStatement progressStmt = conn.prepareStatement(deleteProgress);
                progressStmt.setInt(1, selectedCourse.getCourseId());
                progressStmt.executeUpdate();

                String deleteEnrollments = "DELETE FROM enrollments WHERE course_id = ?";
                PreparedStatement enrollStmt = conn.prepareStatement(deleteEnrollments);
                enrollStmt.setInt(1, selectedCourse.getCourseId());
                enrollStmt.executeUpdate();

                // Then delete the course
                String sql = "DELETE FROM courses WHERE course_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedCourse.getCourseId());
                stmt.executeUpdate();

                loadCourses();
                updateQuickStats();
                showAlert("Success", "Course deleted successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete course: " + e.getMessage());
            }
        }
    }

    @FXML
    private void assignInstructor() {
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showAlert("Error", "Please select a course to assign instructor.");
            return;
        }

        // Get list of instructors
        ObservableList<String> instructors = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username 
                FROM users u JOIN roles r ON u.role_id = r.role_id 
                WHERE r.role_name = 'Instructor'
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                instructors.add(rs.getInt("user_id") + " - " + rs.getString("username"));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load instructors: " + e.getMessage());
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(null, instructors);
        dialog.setTitle("Assign Instructor");
        dialog.setHeaderText("Assign instructor to: " + selectedCourse.getCourseName());
        dialog.setContentText("Choose instructor:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int instructorId = Integer.parseInt(result.get().split(" - ")[0]);

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE courses SET instructor_id = ? WHERE course_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, instructorId);
                    stmt.setInt(2, selectedCourse.getCourseId());
                    stmt.executeUpdate();

                    loadCourses();
                    showAlert("Success", "Instructor assigned successfully!");
                }
            } catch (SQLException | NumberFormatException e) {
                showAlert("Error", "Failed to assign instructor: " + e.getMessage());
            }
        }
    }

    @FXML
    private void manageEnrollments() {
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showAlert("Error", "Please select a course to manage enrollments.");
            return;
        }

        Stage enrollmentStage = new Stage();
        enrollmentStage.setTitle("Manage Enrollments - " + selectedCourse.getCourseName());
        enrollmentStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("Enrolled Students");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Table for enrolled students
        TableView<User> enrolledTable = new TableView<>();
        TableColumn<User, Number> idCol = new TableColumn<>("ID");
        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        TableColumn<User, String> roleCol = new TableColumn<>("Role");

        idCol.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
        usernameCol.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        roleCol.setCellValueFactory(cellData -> cellData.getValue().roleProperty());

        enrolledTable.getColumns().addAll(idCol, usernameCol, roleCol);

        // Load enrolled students
        ObservableList<User> enrolledStudents = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username, r.role_name
                FROM users u 
                JOIN roles r ON u.role_id = r.role_id
                JOIN enrollments e ON u.user_id = e.user_id
                WHERE e.course_id = ?
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourse.getCourseId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                enrolledStudents.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role_name")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load enrolled students: " + e.getMessage());
        }

        enrolledTable.setItems(enrolledStudents);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button enrollButton = new Button("Enroll Student");
        Button unenrollButton = new Button("Unenroll Student");
        Button updateProgressButton = new Button("Update Progress");

        enrollButton.setOnAction(e -> {
            // Show dialog to select student to enroll
            ObservableList<String> availableStudents = FXCollections.observableArrayList();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = """
                    SELECT u.user_id, u.username 
                    FROM users u JOIN roles r ON u.role_id = r.role_id 
                    WHERE r.role_name = 'Student' 
                    AND u.user_id NOT IN (SELECT user_id FROM enrollments WHERE course_id = ?)
                    """;
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selectedCourse.getCourseId());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    availableStudents.add(rs.getInt("user_id") + " - " + rs.getString("username"));
                }
            } catch (SQLException ex) {
                showAlert("Error", "Failed to load available students: " + ex.getMessage());
                return;
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(null, availableStudents);
            dialog.setTitle("Enroll Student");
            dialog.setHeaderText("Select student to enroll in: " + selectedCourse.getCourseName());
            dialog.setContentText("Choose student:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    int studentId = Integer.parseInt(result.get().split(" - ")[0]);

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "INSERT INTO enrollments (user_id, course_id) VALUES (?, ?)";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, selectedCourse.getCourseId());
                        stmt.executeUpdate();

                        // Also create initial progress record
                        String progressSql = "INSERT INTO progress (user_id, course_id, progress_percentage) VALUES (?, ?, 0.0)";
                        PreparedStatement progressStmt = conn.prepareStatement(progressSql);
                        progressStmt.setInt(1, studentId);
                        progressStmt.setInt(2, selectedCourse.getCourseId());
                        progressStmt.executeUpdate();

                        showAlert("Success", "Student enrolled successfully!");
                        enrollmentStage.close();
                        loadCourses(); // Refresh course table
                    }
                } catch (SQLException | NumberFormatException ex) {
                    showAlert("Error", "Failed to enroll student: " + ex.getMessage());
                }
            }
        });

        unenrollButton.setOnAction(e -> {
            User selectedStudent = enrolledTable.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                showAlert("Error", "Please select a student to unenroll.");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Unenrollment");
            confirmAlert.setHeaderText("Unenroll Student");
            confirmAlert.setContentText("Are you sure you want to unenroll " + selectedStudent.getUsername() + "?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    // Delete progress first
                    String progressSql = "DELETE FROM progress WHERE user_id = ? AND course_id = ?";
                    PreparedStatement progressStmt = conn.prepareStatement(progressSql);
                    progressStmt.setInt(1, selectedStudent.getUserId());
                    progressStmt.setInt(2, selectedCourse.getCourseId());
                    progressStmt.executeUpdate();

                    // Then delete enrollment
                    String sql = "DELETE FROM enrollments WHERE user_id = ? AND course_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, selectedStudent.getUserId());
                    stmt.setInt(2, selectedCourse.getCourseId());
                    stmt.executeUpdate();

                    showAlert("Success", "Student unenrolled successfully!");
                    enrollmentStage.close();
                    loadCourses(); // Refresh course table
                } catch (SQLException ex) {
                    showAlert("Error", "Failed to unenroll student: " + ex.getMessage());
                }
            }
        });

        updateProgressButton.setOnAction(e -> {
            User selectedStudent = enrolledTable.getSelectionModel().getSelectedItem();
            if (selectedStudent == null) {
                showAlert("Error", "Please select a student to update progress.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Update Progress");
            dialog.setHeaderText("Update progress for: " + selectedStudent.getUsername());
            dialog.setContentText("Enter progress percentage (0-100):");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    double progress = Double.parseDouble(result.get());
                    if (progress < 0 || progress > 100) {
                        showAlert("Error", "Progress must be between 0 and 100.");
                        return;
                    }

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "UPDATE progress SET progress_percentage = ? WHERE user_id = ? AND course_id = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setDouble(1, progress);
                        stmt.setInt(2, selectedStudent.getUserId());
                        stmt.setInt(3, selectedCourse.getCourseId());
                        stmt.executeUpdate();

                        showAlert("Success", "Progress updated successfully!");
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Error", "Please enter a valid number.");
                } catch (SQLException ex) {
                    showAlert("Error", "Failed to update progress: " + ex.getMessage());
                }
            }
        });

        buttonBox.getChildren().addAll(enrollButton, unenrollButton, updateProgressButton);

        root.getChildren().addAll(title, enrolledTable, buttonBox);

        Scene scene = new Scene(root, 600, 400);
        enrollmentStage.setScene(scene);
        enrollmentStage.showAndWait();
    }

    // ==================== NAVIGATION FUNCTIONS ====================

    @FXML
    private void showUserManagement() {
        hideAllPanes();
        userManagementPane.setVisible(true);
        loadUsers();
        addFadeTransition(userManagementPane);
    }

    @FXML
    private void showCourseManagement() {
        hideAllPanes();
        courseManagementPane.setVisible(true);
        loadCourses();
        addFadeTransition(courseManagementPane);
    }

    @FXML
    private void showReports() {
        showReportsDialog();
    }

    private void showDashboard() {
        hideAllPanes();
        dashboardPane.setVisible(true);
        updateSystemHealth();
        addFadeTransition(dashboardPane);
    }

    private void hideAllPanes() {
        dashboardPane.setVisible(false);
        userManagementPane.setVisible(false);
        courseManagementPane.setVisible(false);
    }

    // ==================== UTILITY FUNCTIONS ====================

    private void updateQuickStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total users
            String userCountSql = "SELECT COUNT(*) FROM users";
            PreparedStatement userStmt = conn.prepareStatement(userCountSql);
            ResultSet userRs = userStmt.executeQuery();
            if (userRs.next()) {
                totalUsersLabel.setText("Total Users: " + userRs.getInt(1));
            }

            // Total courses
            String courseCountSql = "SELECT COUNT(*) FROM courses";
            PreparedStatement courseStmt = conn.prepareStatement(courseCountSql);
            ResultSet courseRs = courseStmt.executeQuery();
            if (courseRs.next()) {
                totalCoursesLabel.setText("Total Courses: " + courseRs.getInt(1));
            }

            // Total enrollments
            String enrollmentCountSql = "SELECT COUNT(*) FROM enrollments";
            PreparedStatement enrollmentStmt = conn.prepareStatement(enrollmentCountSql);
            ResultSet enrollmentRs = enrollmentStmt.executeQuery();
            if (enrollmentRs.next()) {
                totalEnrollmentsLabel.setText("Total Enrollments: " + enrollmentRs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSystemHealth() {
        // Simulate system health check
        systemHealthIndicator.setProgress(0.85);
        systemHealthLabel.setText("Good");

        // Update overall progress
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT AVG(progress_percentage) as avg_progress FROM progress";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double avgProgress = rs.getDouble("avg_progress") / 100.0;
                overallProgressBar.setProgress(avgProgress);
                overallProgressLabel.setText(String.format("%.1f%%", avgProgress * 100));
            }
        } catch (SQLException e) {
            overallProgressBar.setProgress(0.0);
            overallProgressLabel.setText("0%");
        }
    }

    private void setupEventHandlers() {
        // Search functionality
        userSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue, roleFilterCombo.getValue());
        });

        courseSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses(newValue);
        });
    }

    @FXML
    private void filterUsersByRole() {
        filterUsers(userSearchField.getText(), roleFilterCombo.getValue());
    }

    @FXML
    private void searchUsers() {
        filterUsers(userSearchField.getText(), roleFilterCombo.getValue());
    }

    @FXML
    private void searchCourses() {
        filterCourses(courseSearchField.getText());
    }

    private void filterUsers(String searchText, String roleFilter) {
        ObservableList<User> filteredUsers = FXCollections.observableArrayList();

        for (User user : allUsers) {
            boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText.toLowerCase());

            boolean matchesRole = roleFilter == null || roleFilter.equals("All") ||
                    user.getRole().equals(roleFilter);

            if (matchesSearch && matchesRole) {
                filteredUsers.add(user);
            }
        }

        userTable.setItems(filteredUsers);
    }

    private void filterCourses(String searchText) {
        ObservableList<Course> filteredCourses = FXCollections.observableArrayList();

        for (Course course : allCourses) {
            boolean matches = searchText == null || searchText.isEmpty() ||
                    course.getCourseName().toLowerCase().contains(searchText.toLowerCase()) ||
                    course.getInstructor().toLowerCase().contains(searchText.toLowerCase());

            if (matches) {
                filteredCourses.add(course);
            }
        }

        courseTable.setItems(filteredCourses);
    }

    private void showReportsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reports & Analytics");
        alert.setHeaderText("System Reports");

        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder report = new StringBuilder();

            // User statistics
            String userStats = """
                SELECT r.role_name, COUNT(*) as count 
                FROM users u JOIN roles r ON u.role_id = r.role_id 
                GROUP BY r.role_name
                """;
            PreparedStatement stmt = conn.prepareStatement(userStats);
            ResultSet rs = stmt.executeQuery();

            report.append("USER STATISTICS:\n");
            while (rs.next()) {
                report.append(String.format("- %s: %d\n", rs.getString("role_name"), rs.getInt("count")));
            }

            // Course statistics
            String courseStats = "SELECT COUNT(*) as total_courses FROM courses";
            stmt = conn.prepareStatement(courseStats);
            rs = stmt.executeQuery();
            if (rs.next()) {
                report.append(String.format("\nCOURSE STATISTICS:\n- Total Courses: %d\n", rs.getInt("total_courses")));
            }

            // Enrollment statistics
            String enrollStats = """
                SELECT COUNT(*) as total_enrollments, 
                       COUNT(DISTINCT user_id) as unique_students 
                FROM enrollments
                """;
            stmt = conn.prepareStatement(enrollStats);
            rs = stmt.executeQuery();
            if (rs.next()) {
                report.append(String.format("\nENROLLMENT STATISTICS:\n- Total Enrollments: %d\n- Unique Students: %d\n",
                        rs.getInt("total_enrollments"), rs.getInt("unique_students")));
            }

            // Progress statistics
            String progressStats = """
                SELECT AVG(progress_percentage) as avg_progress,
                       MIN(progress_percentage) as min_progress,
                       MAX(progress_percentage) as max_progress
                FROM progress
                """;
            stmt = conn.prepareStatement(progressStats);
            rs = stmt.executeQuery();
            if (rs.next()) {
                report.append(String.format("\nPROGRESS STATISTICS:\n- Average Progress: %.1f%%\n- Minimum Progress: %.1f%%\n- Maximum Progress: %.1f%%\n",
                        rs.getDouble("avg_progress"), rs.getDouble("min_progress"), rs.getDouble("max_progress")));
            }

            alert.setContentText(report.toString());
        } catch (SQLException e) {
            alert.setContentText("Error generating report: " + e.getMessage());
        }

        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/learningmanagement/learningmanagement/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void addButtonEffects() {
        Button[] buttons = {logoutButton, createUserButton, updateUserButton, deleteUserButton,
                resetPasswordButton, createCourseButton, updateCourseButton,
                deleteCourseButton, assignInstructorButton, manageEnrollmentsButton};

        for (Button button : buttons) {
            if (button != null) {
                button.setOnMouseEntered(e -> {
                    button.setScaleX(1.05);
                    button.setScaleY(1.05);
                });
                button.setOnMouseExited(e -> {
                    button.setScaleX(1.0);
                    button.setScaleY(1.0);
                });
            }
        }
    }

    private void addFadeTransition(VBox pane) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), pane);
        fade.setFromValue(0.5);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== MODEL CLASSES ====================

    public static class User {
        private final SimpleIntegerProperty userId;
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;

        public User(int userId, String username, String role) {
            this.userId = new SimpleIntegerProperty(userId);
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
        }

        // Getters and Property methods
        public int getUserId() { return userId.get(); }
        public SimpleIntegerProperty userIdProperty() { return userId; }

        public String getUsername() { return username.get(); }
        public SimpleStringProperty usernameProperty() { return username; }

        public String getRole() { return role.get(); }
        public SimpleStringProperty roleProperty() { return role; }
    }

    public static class Course {
        private final SimpleIntegerProperty courseId;
        private final SimpleStringProperty courseName;
        private final SimpleStringProperty instructor;
        private final SimpleIntegerProperty enrolledCount;

        public Course(int courseId, String courseName, String instructor, int enrolledCount) {
            this.courseId = new SimpleIntegerProperty(courseId);
            this.courseName = new SimpleStringProperty(courseName);
            this.instructor = new SimpleStringProperty(instructor);
            this.enrolledCount = new SimpleIntegerProperty(enrolledCount);
        }

        // Getters and Property methods
        public int getCourseId() { return courseId.get(); }
        public SimpleIntegerProperty courseIdProperty() { return courseId; }

        public String getCourseName() { return courseName.get(); }
        public SimpleStringProperty courseNameProperty() { return courseName; }

        public String getInstructor() { return instructor.get(); }
        public SimpleStringProperty instructorProperty() { return instructor; }

        public int getEnrolledCount() { return enrolledCount.get(); }
        public SimpleIntegerProperty enrolledCountProperty() { return enrolledCount; }
    }
}