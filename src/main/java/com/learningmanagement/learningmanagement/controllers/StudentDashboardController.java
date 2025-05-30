package com.learningmanagement.learningmanagement.controllers;

import com.learningmanagement.learningmanagement.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class StudentDashboardController {

    // Main UI Components
    @FXML private StackPane contentStackPane;
    @FXML private VBox dashboardPane;
    @FXML private VBox myCoursesPane;
    @FXML private VBox availableCoursesPane;
    @FXML private VBox courseMaterialsPane;
    @FXML private VBox assignmentsPane;
    @FXML private VBox submissionPane;
    @FXML private VBox gradesPane;
    @FXML private VBox messagesPane;

    // Dashboard components
    @FXML private Label currentStudentLabel;
    @FXML private Label enrolledCoursesLabel;
    @FXML private Label unreadMessagesLabel;
    @FXML private Label assignmentsDueLabel;
    @FXML private Label averageGradeLabel;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label overallProgressLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label progressLabel;

    // My Courses components
    @FXML private ScrollPane courseScrollPane;
    @FXML private VBox courseList;
    @FXML private Pagination pagination;

    // Available Courses components
    @FXML private TableView<AvailableCourse> availableCoursesTable;
    @FXML private TableColumn<AvailableCourse, Number> courseIdColumn;
    @FXML private TableColumn<AvailableCourse, String> courseNameColumn;
    @FXML private TableColumn<AvailableCourse, String> instructorColumn;
    @FXML private TableColumn<AvailableCourse, Number> enrolledCountColumn;

    // Course Materials components
    @FXML private TableView<CourseMaterial> materialsTable;
    @FXML private TableColumn<CourseMaterial, String> materialNameColumn;
    @FXML private TableColumn<CourseMaterial, String> materialTypeColumn;
    @FXML private TableColumn<CourseMaterial, String> materialTopicColumn;
    @FXML private TableColumn<CourseMaterial, String> materialDateColumn;
    @FXML private ComboBox<String> materialCourseFilter;

    // Assignments components
    @FXML private TableView<StudentAssignment> assignmentsTable;
    @FXML private TableColumn<StudentAssignment, String> assignmentNameColumn;
    @FXML private TableColumn<StudentAssignment, String> assignmentCourseColumn;
    @FXML private TableColumn<StudentAssignment, String> assignmentDueDateColumn;
    @FXML private TableColumn<StudentAssignment, Number> assignmentMaxPointsColumn;
    @FXML private TableColumn<StudentAssignment, String> assignmentStatusColumn;
    @FXML private TableColumn<StudentAssignment, Number> assignmentGradeColumn;
    @FXML private ComboBox<String> assignmentCourseFilter;
    @FXML private ComboBox<String> assignmentStatusFilter;

    // Submission components
    @FXML private ComboBox<String> submissionAssignmentCombo;
    @FXML private TextArea submissionTextArea;
    @FXML private TextField selectedFileField;
    @FXML private Button selectFileButton;

    // Grades components
    @FXML private TableView<StudentGrade> gradesTable;
    @FXML private TableColumn<StudentGrade, String> gradeAssignmentColumn;
    @FXML private TableColumn<StudentGrade, String> gradeCourseColumn;
    @FXML private TableColumn<StudentGrade, Number> gradePointsColumn;
    @FXML private TableColumn<StudentGrade, Number> gradeMaxPointsColumn;
    @FXML private TableColumn<StudentGrade, String> gradePercentageColumn;
    @FXML private TableColumn<StudentGrade, String> gradeFeedbackColumn;
    @FXML private Label overallAverageLabel;
    @FXML private Label gradedAssignmentsLabel;
    @FXML private Label pendingGradesLabel;

    // Messages components
    @FXML private TableView<StudentMessage> messagesTable;
    @FXML private TableColumn<StudentMessage, String> messageTypeColumn;
    @FXML private TableColumn<StudentMessage, String> messageSenderColumn;
    @FXML private TableColumn<StudentMessage, String> messageSubjectColumn;
    @FXML private TableColumn<StudentMessage, String> messageContentColumn;
    @FXML private TableColumn<StudentMessage, String> messageDateColumn;
    @FXML private TableColumn<StudentMessage, String> messageReadColumn;

    // Button references
    @FXML private Button enrollButton;
    @FXML private Button logoutButton;

    private ObservableList<AvailableCourse> allAvailableCourses = FXCollections.observableArrayList();
    private ObservableList<CourseMaterial> allMaterials = FXCollections.observableArrayList();
    private ObservableList<StudentAssignment> allAssignments = FXCollections.observableArrayList();
    private ObservableList<StudentGrade> allGrades = FXCollections.observableArrayList();
    private ObservableList<StudentMessage> allMessages = FXCollections.observableArrayList();

    private static final int ITEMS_PER_PAGE = 5;
    private int currentStudentId = 3; // Default value, will be set from login
    private File selectedSubmissionFile = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupEventHandlers();
        setupVisualEffects();

        // Don't load data here - wait for setCurrentStudentId to be called
        showDashboard();

        // Initial fade in
        if (contentStackPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(800), contentStackPane);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    // ==================== SETTER METHOD FOR LOGIN ====================

    /**
     * Sets the current student ID and loads all data
     * This method is called from LoginController after successful login
     */
    public void setCurrentStudentId(int studentId) {
        this.currentStudentId = studentId;

        // Update the welcome message
        updateCurrentStudentLabel();

        // Load all data with the new student ID
        loadInitialData();
        updateDashboardStats();
        setupPagination();
    }

    private void updateCurrentStudentLabel() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT username, full_name FROM users WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("full_name");
                String username = rs.getString("username");
                String displayName = (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;

                if (currentStudentLabel != null) {
                    currentStudentLabel.setText("Welcome, " + displayName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (currentStudentLabel != null) {
                currentStudentLabel.setText("Welcome, Student");
            }
        }
    }

    private void setupTableColumns() {
        // Available Courses table
        if (courseIdColumn != null) courseIdColumn.setCellValueFactory(cellData -> cellData.getValue().courseIdProperty());
        if (courseNameColumn != null) courseNameColumn.setCellValueFactory(cellData -> cellData.getValue().courseNameProperty());
        if (instructorColumn != null) instructorColumn.setCellValueFactory(cellData -> cellData.getValue().instructorProperty());
        if (enrolledCountColumn != null) enrolledCountColumn.setCellValueFactory(cellData -> cellData.getValue().enrolledCountProperty());

        // Materials table
        if (materialNameColumn != null) materialNameColumn.setCellValueFactory(cellData -> cellData.getValue().materialNameProperty());
        if (materialTypeColumn != null) materialTypeColumn.setCellValueFactory(cellData -> cellData.getValue().materialTypeProperty());
        if (materialTopicColumn != null) materialTopicColumn.setCellValueFactory(cellData -> cellData.getValue().topicNameProperty());
        if (materialDateColumn != null) materialDateColumn.setCellValueFactory(cellData -> cellData.getValue().uploadDateProperty());

        // Assignments table
        if (assignmentNameColumn != null) assignmentNameColumn.setCellValueFactory(cellData -> cellData.getValue().assignmentNameProperty());
        if (assignmentCourseColumn != null) assignmentCourseColumn.setCellValueFactory(cellData -> cellData.getValue().courseNameProperty());
        if (assignmentDueDateColumn != null) assignmentDueDateColumn.setCellValueFactory(cellData -> cellData.getValue().dueDateProperty());
        if (assignmentMaxPointsColumn != null) assignmentMaxPointsColumn.setCellValueFactory(cellData -> cellData.getValue().maxPointsProperty());
        if (assignmentStatusColumn != null) assignmentStatusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        if (assignmentGradeColumn != null) assignmentGradeColumn.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());

        // Grades table
        if (gradeAssignmentColumn != null) gradeAssignmentColumn.setCellValueFactory(cellData -> cellData.getValue().assignmentNameProperty());
        if (gradeCourseColumn != null) gradeCourseColumn.setCellValueFactory(cellData -> cellData.getValue().courseNameProperty());
        if (gradePointsColumn != null) gradePointsColumn.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        if (gradeMaxPointsColumn != null) gradeMaxPointsColumn.setCellValueFactory(cellData -> cellData.getValue().maxPointsProperty());
        if (gradePercentageColumn != null) gradePercentageColumn.setCellValueFactory(cellData -> cellData.getValue().percentageProperty());
        if (gradeFeedbackColumn != null) gradeFeedbackColumn.setCellValueFactory(cellData -> cellData.getValue().feedbackProperty());

        // Messages table
        if (messageTypeColumn != null) messageTypeColumn.setCellValueFactory(cellData -> cellData.getValue().messageTypeProperty());
        if (messageSenderColumn != null) messageSenderColumn.setCellValueFactory(cellData -> cellData.getValue().senderProperty());
        if (messageSubjectColumn != null) messageSubjectColumn.setCellValueFactory(cellData -> cellData.getValue().subjectProperty());
        if (messageContentColumn != null) messageContentColumn.setCellValueFactory(cellData -> cellData.getValue().messageTextProperty());
        if (messageDateColumn != null) messageDateColumn.setCellValueFactory(cellData -> cellData.getValue().sentDateProperty());
        if (messageReadColumn != null) messageReadColumn.setCellValueFactory(cellData -> cellData.getValue().readStatusProperty());
    }

    private void setupEventHandlers() {
        // Setup filter combo boxes
        if (assignmentStatusFilter != null) {
            assignmentStatusFilter.setItems(FXCollections.observableArrayList(
                    "All", "Not Submitted", "Submitted", "Graded", "Overdue"
            ));
            assignmentStatusFilter.setValue("All");
        }
    }

    private void setupVisualEffects() {
        // Animated enroll button
        if (enrollButton != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(2000), enrollButton);
            fade.setFromValue(1.0);
            fade.setToValue(0.6);
            fade.setCycleCount(FadeTransition.INDEFINITE);
            fade.setAutoReverse(true);
            fade.play();
        }

        // Button hover effects
        Button[] buttons = {logoutButton, enrollButton};
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

    private void setupPagination() {
        if (pagination == null) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM enrollments WHERE user_id = ? AND status = 'APPROVED'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                pagination.setPageCount(Math.max(1, (int) Math.ceil((double) count / ITEMS_PER_PAGE)));
            }
            pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> loadCourses(newIndex.intValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInitialData() {
        loadAvailableCourses();
        loadCourseMaterials();
        loadAssignments();
        loadGrades();
        loadMessages();
        loadCourseFilters();
        loadSubmissionAssignments();
    }

    // ==================== DASHBOARD ====================

    private void updateDashboardStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Enrolled courses count (only approved)
            String enrolledSql = "SELECT COUNT(*) FROM enrollments WHERE user_id = ? AND status = 'APPROVED'";
            PreparedStatement enrolledStmt = conn.prepareStatement(enrolledSql);
            enrolledStmt.setInt(1, currentStudentId);
            ResultSet enrolledRs = enrolledStmt.executeQuery();
            if (enrolledRs.next() && enrolledCoursesLabel != null) {
                enrolledCoursesLabel.setText("Enrolled Courses: " + enrolledRs.getInt(1));
            }

            // Unread messages count
            String unreadSql = "SELECT COUNT(*) FROM messages WHERE recipient_id = ? AND is_read = false";
            PreparedStatement unreadStmt = conn.prepareStatement(unreadSql);
            unreadStmt.setInt(1, currentStudentId);
            ResultSet unreadRs = unreadStmt.executeQuery();
            if (unreadRs.next() && unreadMessagesLabel != null) {
                unreadMessagesLabel.setText("Unread Messages: " + unreadRs.getInt(1));
            }

            // Assignments due this week (only for approved enrollments)
            String dueSql = """
                SELECT COUNT(*) FROM assignments a
                JOIN courses c ON a.course_id = c.course_id
                JOIN enrollments e ON c.course_id = e.course_id
                WHERE e.user_id = ? AND e.status = 'APPROVED' 
                AND a.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
                AND a.assignment_id NOT IN (SELECT assignment_id FROM assignment_submissions WHERE student_id = ?)
                """;
            PreparedStatement dueStmt = conn.prepareStatement(dueSql);
            dueStmt.setInt(1, currentStudentId);
            dueStmt.setInt(2, currentStudentId);
            ResultSet dueRs = dueStmt.executeQuery();
            if (dueRs.next() && assignmentsDueLabel != null) {
                assignmentsDueLabel.setText(String.valueOf(dueRs.getInt(1)));
            }

            // Average grade
            String avgSql = """
                SELECT AVG(s.grade) FROM assignment_submissions s
                JOIN assignments a ON s.assignment_id = a.assignment_id
                WHERE s.student_id = ? AND s.grade IS NOT NULL
                """;
            PreparedStatement avgStmt = conn.prepareStatement(avgSql);
            avgStmt.setInt(1, currentStudentId);
            ResultSet avgRs = avgStmt.executeQuery();
            if (avgRs.next() && averageGradeLabel != null) {
                double avg = avgRs.getDouble(1);
                if (!avgRs.wasNull()) {
                    averageGradeLabel.setText(String.format("%.1f", avg));
                } else {
                    averageGradeLabel.setText("N/A");
                }
            }

            // Overall progress
            String progressSql = "SELECT AVG(progress_percentage) FROM progress WHERE user_id = ?";
            PreparedStatement progressStmt = conn.prepareStatement(progressSql);
            progressStmt.setInt(1, currentStudentId);
            ResultSet progressRs = progressStmt.executeQuery();
            if (progressRs.next()) {
                double progress = progressRs.getDouble(1);
                if (!progressRs.wasNull()) {
                    if (overallProgressBar != null) overallProgressBar.setProgress(progress / 100.0);
                    if (overallProgressLabel != null) overallProgressLabel.setText(String.format("%.1f%%", progress));
                    if (progressIndicator != null) progressIndicator.setProgress(progress / 100.0);
                    if (progressLabel != null) progressLabel.setText(String.format("%.1f%%", progress));
                } else {
                    if (overallProgressBar != null) overallProgressBar.setProgress(0.0);
                    if (overallProgressLabel != null) overallProgressLabel.setText("0.0%");
                    if (progressIndicator != null) progressIndicator.setProgress(0.0);
                    if (progressLabel != null) progressLabel.setText("0.0%");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== MY COURSES ====================

    private void loadCourses(int pageIndex) {
        if (courseList == null) return;

        courseList.getChildren().clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT c.course_name, u.username as instructor, p.progress_percentage 
                FROM enrollments e 
                JOIN courses c ON e.course_id = c.course_id 
                JOIN users u ON c.instructor_id = u.user_id
                LEFT JOIN progress p ON e.user_id = p.user_id AND e.course_id = p.course_id 
                WHERE e.user_id = ? AND e.status = 'APPROVED'
                LIMIT ? OFFSET ?
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, ITEMS_PER_PAGE);
            stmt.setInt(3, pageIndex * ITEMS_PER_PAGE);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("course_name");
                String instructor = rs.getString("instructor");
                double progress = rs.getDouble("progress_percentage");
                if (rs.wasNull()) progress = 0.0;

                // Create course card
                VBox courseCard = new VBox(5);
                courseCard.setStyle("-fx-padding: 15; -fx-background-color: #F5F5F5; -fx-border-radius: 5; -fx-background-radius: 5;");

                Label nameLabel = new Label(courseName);
                nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                Label instructorLabel = new Label("Instructor: " + instructor);
                instructorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

                ProgressBar progressBar = new ProgressBar(progress / 100.0);
                progressBar.setPrefWidth(200);

                Label progressLabel = new Label(String.format("Progress: %.1f%%", progress));
                progressLabel.setStyle("-fx-font-size: 12px;");

                courseCard.getChildren().addAll(nameLabel, instructorLabel, progressBar, progressLabel);
                courseList.getChildren().add(courseCard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showMyCourses() {
        hideAllPanes();
        if (myCoursesPane != null) {
            myCoursesPane.setVisible(true);
            loadCourses(0);
            addFadeTransition(myCoursesPane);
        }
    }

    // ==================== AVAILABLE COURSES ====================

    private void loadAvailableCourses() {
        allAvailableCourses.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT c.course_id, c.course_name, u.username as instructor,
                       COUNT(e.user_id) as enrolled_count
                FROM courses c
                JOIN users u ON c.instructor_id = u.user_id
                LEFT JOIN enrollments e ON c.course_id = e.course_id AND e.status = 'APPROVED'
                WHERE c.course_id NOT IN (
                    SELECT course_id FROM enrollments WHERE user_id = ? AND status IN ('APPROVED', 'PENDING')
                )
                GROUP BY c.course_id, c.course_name, u.username
                ORDER BY c.course_name
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allAvailableCourses.add(new AvailableCourse(
                        rs.getInt("course_id"),
                        rs.getString("course_name"),
                        rs.getString("instructor"),
                        rs.getInt("enrolled_count")
                ));
            }
            if (availableCoursesTable != null) {
                availableCoursesTable.setItems(allAvailableCourses);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load available courses: " + e.getMessage());
        }
    }

    @FXML
    private void showAvailableCourses() {
        hideAllPanes();
        if (availableCoursesPane != null) {
            availableCoursesPane.setVisible(true);
            loadAvailableCourses();
            addFadeTransition(availableCoursesPane);
        }
    }

    @FXML
    private void enrollInCourse() {
        if (availableCoursesTable == null) return;

        AvailableCourse selected = availableCoursesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a course to enroll in.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Enrollment");
        confirmAlert.setHeaderText("Enroll in Course");
        confirmAlert.setContentText("Are you sure you want to enroll in: " + selected.getCourseName() +
                "?\n\nYour enrollment will be pending until approved by an administrator.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Insert with PENDING status
                String sql = "INSERT INTO enrollments (user_id, course_id, status) VALUES (?, ?, 'PENDING')";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, currentStudentId);
                stmt.setInt(2, selected.getCourseId());
                stmt.executeUpdate();

                loadAvailableCourses();
                updateDashboardStats();
                showAlert("Success", "Enrollment request submitted for " + selected.getCourseName() +
                        ".\nYou will be able to access the course once approved by an administrator.");
            } catch (SQLException e) {
                showAlert("Error", "Failed to enroll in course: " + e.getMessage());
            }
        }
    }

    // ==================== COURSE MATERIALS ====================

    private void loadCourseMaterials() {
        allMaterials.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cm.material_name, cm.material_type, cm.upload_date,
                       ct.topic_name, c.course_name
                FROM course_materials cm
                JOIN courses c ON cm.course_id = c.course_id
                LEFT JOIN course_topics ct ON cm.topic_id = ct.topic_id
                JOIN enrollments e ON c.course_id = e.course_id
                WHERE e.user_id = ? AND e.status = 'APPROVED'
                ORDER BY cm.upload_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allMaterials.add(new CourseMaterial(
                        rs.getString("material_name"),
                        rs.getString("material_type"),
                        rs.getString("topic_name") != null ? rs.getString("topic_name") : "No Topic",
                        rs.getTimestamp("upload_date").toString()
                ));
            }
            if (materialsTable != null) {
                materialsTable.setItems(allMaterials);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load course materials: " + e.getMessage());
        }
    }

    private void loadCourseFilters() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT DISTINCT c.course_name
                FROM courses c
                JOIN enrollments e ON c.course_id = e.course_id
                WHERE e.user_id = ? AND e.status = 'APPROVED'
                ORDER BY c.course_name
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> courses = FXCollections.observableArrayList();
            courses.add("All Courses");
            while (rs.next()) {
                courses.add(rs.getString("course_name"));
            }

            if (materialCourseFilter != null) {
                materialCourseFilter.setItems(courses);
                materialCourseFilter.setValue("All Courses");
            }

            if (assignmentCourseFilter != null) {
                assignmentCourseFilter.setItems(courses);
                assignmentCourseFilter.setValue("All Courses");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showCourseMaterials() {
        hideAllPanes();
        if (courseMaterialsPane != null) {
            courseMaterialsPane.setVisible(true);
            loadCourseMaterials();
            addFadeTransition(courseMaterialsPane);
        }
    }

    @FXML
    private void filterMaterialsByCourse() {
        if (materialCourseFilter == null || materialsTable == null) return;

        String selectedCourse = materialCourseFilter.getValue();
        if (selectedCourse == null || selectedCourse.equals("All Courses")) {
            materialsTable.setItems(allMaterials);
        } else {
            ObservableList<CourseMaterial> filtered = FXCollections.observableArrayList();
            // Note: You would need to add course name to CourseMaterial class to implement this properly
            materialsTable.setItems(filtered);
        }
    }

    @FXML
    private void downloadMaterial() {
        if (materialsTable == null) return;

        CourseMaterial selected = materialsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a material to download.");
            return;
        }

        showAlert("Info", "Download functionality would be implemented here for: " + selected.getMaterialName());
    }

    // ==================== ASSIGNMENTS ====================

    private void loadAssignments() {
        allAssignments.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.assignment_id, a.assignment_name, c.course_name, a.due_date, a.max_points,
                       s.grade, s.submission_date,
                       CASE 
                           WHEN s.submission_id IS NULL AND a.due_date < CURRENT_TIMESTAMP THEN 'Overdue'
                           WHEN s.submission_id IS NULL THEN 'Not Submitted'
                           WHEN s.grade IS NULL THEN 'Submitted'
                           ELSE 'Graded'
                       END as status
                FROM assignments a
                JOIN courses c ON a.course_id = c.course_id
                JOIN enrollments e ON c.course_id = e.course_id
                LEFT JOIN assignment_submissions s ON a.assignment_id = s.assignment_id AND s.student_id = ?
                WHERE e.user_id = ? AND e.status = 'APPROVED'
                ORDER BY a.due_date
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String dueDate = rs.getTimestamp("due_date") != null ?
                        rs.getTimestamp("due_date").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                        "No due date";

                double grade = rs.getDouble("grade");
                if (rs.wasNull()) grade = -1; // Use -1 to indicate no grade

                allAssignments.add(new StudentAssignment(
                        rs.getInt("assignment_id"),
                        rs.getString("assignment_name"),
                        rs.getString("course_name"),
                        dueDate,
                        rs.getDouble("max_points"),
                        rs.getString("status"),
                        grade
                ));
            }
            if (assignmentsTable != null) {
                assignmentsTable.setItems(allAssignments);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load assignments: " + e.getMessage());
        }
    }

    @FXML
    private void showAssignments() {
        hideAllPanes();
        if (assignmentsPane != null) {
            assignmentsPane.setVisible(true);
            loadAssignments();
            addFadeTransition(assignmentsPane);
        }
    }

    @FXML
    private void filterAssignmentsByCourse() {
        if (assignmentCourseFilter == null || assignmentsTable == null) return;

        String selectedCourse = assignmentCourseFilter.getValue();
        if (selectedCourse == null || selectedCourse.equals("All Courses")) {
            assignmentsTable.setItems(allAssignments);
        } else {
            ObservableList<StudentAssignment> filtered = FXCollections.observableArrayList();
            for (StudentAssignment assignment : allAssignments) {
                if (assignment.getCourseName().equals(selectedCourse)) {
                    filtered.add(assignment);
                }
            }
            assignmentsTable.setItems(filtered);
        }
    }

    @FXML
    private void filterAssignmentsByStatus() {
        if (assignmentStatusFilter == null || assignmentsTable == null) return;

        String selectedStatus = assignmentStatusFilter.getValue();
        if (selectedStatus == null || selectedStatus.equals("All")) {
            assignmentsTable.setItems(allAssignments);
        } else {
            ObservableList<StudentAssignment> filtered = FXCollections.observableArrayList();
            for (StudentAssignment assignment : allAssignments) {
                if (assignment.getStatus().equals(selectedStatus)) {
                    filtered.add(assignment);
                }
            }
            assignmentsTable.setItems(filtered);
        }
    }

    @FXML
    private void viewAssignmentDetails() {
        if (assignmentsTable == null) return;

        StudentAssignment selected = assignmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an assignment to view details.");
            return;
        }

        showAssignmentDetailsDialog(selected);
    }

    private void showAssignmentDetailsDialog(StudentAssignment assignment) {
        Stage detailStage = new Stage();
        detailStage.setTitle("Assignment Details - " + assignment.getAssignmentName());
        detailStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Assignment Details");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);

        infoGrid.add(new Label("Assignment:"), 0, 0);
        infoGrid.add(new Label(assignment.getAssignmentName()), 1, 0);
        infoGrid.add(new Label("Course:"), 0, 1);
        infoGrid.add(new Label(assignment.getCourseName()), 1, 1);
        infoGrid.add(new Label("Due Date:"), 0, 2);
        infoGrid.add(new Label(assignment.getDueDate()), 1, 2);
        infoGrid.add(new Label("Max Points:"), 0, 3);
        infoGrid.add(new Label(String.valueOf(assignment.getMaxPoints())), 1, 3);
        infoGrid.add(new Label("Status:"), 0, 4);
        infoGrid.add(new Label(assignment.getStatus()), 1, 4);

        if (assignment.getGrade() >= 0) {
            infoGrid.add(new Label("Grade:"), 0, 5);
            infoGrid.add(new Label(String.format("%.1f / %.1f", assignment.getGrade(), assignment.getMaxPoints())), 1, 5);
        }

        // Load assignment description
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT description FROM assignments WHERE assignment_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, assignment.getAssignmentId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String description = rs.getString("description");
                if (description != null && !description.isEmpty()) {
                    Label descLabel = new Label("Description:");
                    descLabel.setStyle("-fx-font-weight: bold;");
                    TextArea descArea = new TextArea(description);
                    descArea.setEditable(false);
                    descArea.setPrefRowCount(3);
                    root.getChildren().addAll(descLabel, descArea);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        root.getChildren().addAll(title, infoGrid);

        Scene scene = new Scene(root, 500, 400);
        detailStage.setScene(scene);
        detailStage.showAndWait();
    }

    @FXML
    private void submitAssignment() {
        if (assignmentsTable == null) return;

        StudentAssignment selected = assignmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an assignment to submit.");
            return;
        }

        if (!selected.getStatus().equals("Not Submitted")) {
            showAlert("Info", "This assignment has already been submitted.");
            return;
        }

        showSubmissionForm();
        // Pre-select the assignment in the submission form
        if (submissionAssignmentCombo != null) {
            submissionAssignmentCombo.setValue(selected.getAssignmentName());
        }
    }

    // ==================== ASSIGNMENT SUBMISSION ====================

    private void loadSubmissionAssignments() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.assignment_id, a.assignment_name, c.course_name
                FROM assignments a
                JOIN courses c ON a.course_id = c.course_id
                JOIN enrollments e ON c.course_id = e.course_id
                WHERE e.user_id = ? AND e.status = 'APPROVED' AND a.assignment_id NOT IN (
                    SELECT assignment_id FROM assignment_submissions WHERE student_id = ?
                )
                ORDER BY a.due_date
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> assignments = FXCollections.observableArrayList();
            while (rs.next()) {
                assignments.add(rs.getString("assignment_name") + " (" + rs.getString("course_name") + ")");
            }
            if (submissionAssignmentCombo != null) {
                submissionAssignmentCombo.setItems(assignments);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showSubmissionForm() {
        hideAllPanes();
        if (submissionPane != null) {
            submissionPane.setVisible(true);
            loadSubmissionAssignments();
            addFadeTransition(submissionPane);
        }
    }

    @FXML
    private void selectFile() {
        if (submissionPane == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assignment File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        selectedSubmissionFile = fileChooser.showOpenDialog(submissionPane.getScene().getWindow());
        if (selectedSubmissionFile != null && selectedFileField != null) {
            selectedFileField.setText(selectedSubmissionFile.getName());
        }
    }

    @FXML
    private void submitAssignmentForm() {
        if (submissionAssignmentCombo == null || submissionTextArea == null) return;

        String selectedAssignment = submissionAssignmentCombo.getValue();
        String submissionText = submissionTextArea.getText();

        if (selectedAssignment == null) {
            showAlert("Error", "Please select an assignment.");
            return;
        }

        if (submissionText.trim().isEmpty() && selectedSubmissionFile == null) {
            showAlert("Error", "Please provide either text submission or upload a file.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get assignment ID
            String assignmentName = selectedAssignment.split(" \\(")[0];
            String getAssignmentIdSql = "SELECT assignment_id FROM assignments WHERE assignment_name = ?";
            PreparedStatement getIdStmt = conn.prepareStatement(getAssignmentIdSql);
            getIdStmt.setString(1, assignmentName);
            ResultSet idRs = getIdStmt.executeQuery();

            if (!idRs.next()) {
                showAlert("Error", "Assignment not found.");
                return;
            }

            int assignmentId = idRs.getInt("assignment_id");

            // Handle file upload if present
            String filePath = null;
            if (selectedSubmissionFile != null) {
                try {
                    Path uploadsDir = Paths.get("submissions");
                    if (!Files.exists(uploadsDir)) {
                        Files.createDirectories(uploadsDir);
                    }

                    String fileName = System.currentTimeMillis() + "_" + selectedSubmissionFile.getName();
                    Path targetPath = uploadsDir.resolve(fileName);
                    Files.copy(selectedSubmissionFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    filePath = targetPath.toString();
                } catch (IOException e) {
                    showAlert("Error", "Failed to upload file: " + e.getMessage());
                    return;
                }
            }

            // Insert submission
            String sql = "INSERT INTO assignment_submissions (assignment_id, student_id, submission_text, file_path) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, assignmentId);
            stmt.setInt(2, currentStudentId);
            stmt.setString(3, submissionText.trim().isEmpty() ? null : submissionText);
            stmt.setString(4, filePath);
            stmt.executeUpdate();

            clearSubmissionForm();
            loadAssignments();
            loadSubmissionAssignments();
            updateDashboardStats();
            showAlert("Success", "Assignment submitted successfully!");

        } catch (SQLException e) {
            showAlert("Error", "Failed to submit assignment: " + e.getMessage());
        }
    }

    @FXML
    private void clearSubmissionForm() {
        if (submissionAssignmentCombo != null) submissionAssignmentCombo.setValue(null);
        if (submissionTextArea != null) submissionTextArea.clear();
        if (selectedFileField != null) selectedFileField.clear();
        selectedSubmissionFile = null;
    }

    // ==================== GRADES ====================

    private void loadGrades() {
        allGrades.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.assignment_name, c.course_name, s.grade, a.max_points, s.feedback
                FROM assignment_submissions s
                JOIN assignments a ON s.assignment_id = a.assignment_id
                JOIN courses c ON a.course_id = c.course_id
                WHERE s.student_id = ? AND s.grade IS NOT NULL
                ORDER BY s.graded_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                double grade = rs.getDouble("grade");
                double maxPoints = rs.getDouble("max_points");
                double percentage = (grade / maxPoints) * 100;

                allGrades.add(new StudentGrade(
                        rs.getString("assignment_name"),
                        rs.getString("course_name"),
                        grade,
                        maxPoints,
                        String.format("%.1f%%", percentage),
                        rs.getString("feedback") != null ? rs.getString("feedback") : "No feedback"
                ));
            }
            if (gradesTable != null) {
                gradesTable.setItems(allGrades);
            }
            updateGradeStatistics();
        } catch (SQLException e) {
            showAlert("Error", "Failed to load grades: " + e.getMessage());
        }
    }

    private void updateGradeStatistics() {
        if (allGrades.isEmpty()) {
            if (overallAverageLabel != null) overallAverageLabel.setText("0.0%");
            if (gradedAssignmentsLabel != null) gradedAssignmentsLabel.setText("0");
            if (pendingGradesLabel != null) pendingGradesLabel.setText("0");
            return;
        }

        // Calculate overall average
        double totalPercentage = 0;
        for (StudentGrade grade : allGrades) {
            String percentageStr = grade.getPercentage().replace("%", "");
            totalPercentage += Double.parseDouble(percentageStr);
        }
        double average = totalPercentage / allGrades.size();
        if (overallAverageLabel != null) {
            overallAverageLabel.setText(String.format("%.1f%%", average));
        }

        // Graded assignments count
        if (gradedAssignmentsLabel != null) {
            gradedAssignmentsLabel.setText(String.valueOf(allGrades.size()));
        }

        // Pending grades count
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT COUNT(*) FROM assignment_submissions s
                JOIN assignments a ON s.assignment_id = a.assignment_id
                JOIN courses c ON a.course_id = c.course_id
                JOIN enrollments e ON c.course_id = e.course_id
                WHERE e.user_id = ? AND s.student_id = ? AND s.grade IS NULL AND e.status = 'APPROVED'
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && pendingGradesLabel != null) {
                pendingGradesLabel.setText(String.valueOf(rs.getInt(1)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showGrades() {
        hideAllPanes();
        if (gradesPane != null) {
            gradesPane.setVisible(true);
            loadGrades();
            addFadeTransition(gradesPane);
        }
    }

    // ==================== MESSAGES ====================

    private void loadMessages() {
        allMessages.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT m.message_id, m.message_type, u.username as sender, m.subject, 
                       m.message_text, m.sent_date, m.is_read
                FROM messages m
                LEFT JOIN users u ON m.sender_id = u.user_id
                WHERE m.recipient_id = ? OR (m.recipient_id IS NULL AND m.course_id IN (
                    SELECT course_id FROM enrollments WHERE user_id = ? AND status = 'APPROVED'
                ))
                ORDER BY m.sent_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allMessages.add(new StudentMessage(
                        rs.getInt("message_id"),
                        rs.getString("message_type"),
                        rs.getString("sender") != null ? rs.getString("sender") : "System",
                        rs.getString("subject"),
                        rs.getString("message_text"),
                        rs.getTimestamp("sent_date").toString(),
                        rs.getBoolean("is_read") ? "✓" : "✗"
                ));
            }
            if (messagesTable != null) {
                messagesTable.setItems(allMessages);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load messages: " + e.getMessage());
        }
    }

    @FXML
    private void showMessages() {
        hideAllPanes();
        if (messagesPane != null) {
            messagesPane.setVisible(true);
            loadMessages();
            addFadeTransition(messagesPane);
        }
    }

    @FXML
    private void markMessageRead() {
        if (messagesTable == null) return;

        StudentMessage selected = messagesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a message to mark as read.");
            return;
        }

        if (selected.getReadStatus().equals("✓")) {
            showAlert("Info", "Message is already marked as read.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE messages SET is_read = true WHERE message_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selected.getMessageId());
            stmt.executeUpdate();

            loadMessages();
            updateDashboardStats();
            showAlert("Success", "Message marked as read.");
        } catch (SQLException e) {
            showAlert("Error", "Failed to mark message as read: " + e.getMessage());
        }
    }

    @FXML
    private void markAllMessagesRead() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                UPDATE messages SET is_read = true 
                WHERE (recipient_id = ? OR (recipient_id IS NULL AND course_id IN (
                    SELECT course_id FROM enrollments WHERE user_id = ? AND status = 'APPROVED'
                ))) AND is_read = false
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, currentStudentId);
            int updated = stmt.executeUpdate();

            loadMessages();
            updateDashboardStats();
            showAlert("Success", updated + " messages marked as read.");
        } catch (SQLException e) {
            showAlert("Error", "Failed to mark messages as read: " + e.getMessage());
        }
    }

    @FXML
    private void viewMessage() {
        if (messagesTable == null) return;

        StudentMessage selected = messagesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a message to view.");
            return;
        }

        showMessageDialog(selected);
    }

    private void showMessageDialog(StudentMessage message) {
        Stage messageStage = new Stage();
        messageStage.setTitle("Message - " + message.getSubject());
        messageStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Message Details");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);

        infoGrid.add(new Label("From:"), 0, 0);
        infoGrid.add(new Label(message.getSender()), 1, 0);
        infoGrid.add(new Label("Subject:"), 0, 1);
        infoGrid.add(new Label(message.getSubject()), 1, 1);
        infoGrid.add(new Label("Date:"), 0, 2);
        infoGrid.add(new Label(message.getSentDate()), 1, 2);
        infoGrid.add(new Label("Type:"), 0, 3);
        infoGrid.add(new Label(message.getMessageType()), 1, 3);

        Label contentLabel = new Label("Message:");
        contentLabel.setStyle("-fx-font-weight: bold;");

        TextArea contentArea = new TextArea(message.getMessageText());
        contentArea.setEditable(false);
        contentArea.setPrefRowCount(6);
        contentArea.setWrapText(true);

        Button markReadButton = new Button("Mark as Read");
        markReadButton.setOnAction(e -> {
            if (message.getReadStatus().equals("✗")) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE messages SET is_read = true WHERE message_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, message.getMessageId());
                    stmt.executeUpdate();

                    loadMessages();
                    updateDashboardStats();
                    messageStage.close();
                } catch (SQLException ex) {
                    showAlert("Error", "Failed to mark message as read: " + ex.getMessage());
                }
            } else {
                messageStage.close();
            }
        });

        root.getChildren().addAll(title, infoGrid, contentLabel, contentArea, markReadButton);

        Scene scene = new Scene(root, 500, 400);
        messageStage.setScene(scene);
        messageStage.showAndWait();
    }

    // ==================== PENDING ENROLLMENTS ====================

    @FXML
    private void showPendingEnrollments() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT c.course_name, u.username as instructor, e.status, e.enrollment_date
                FROM enrollments e 
                JOIN courses c ON e.course_id = c.course_id 
                JOIN users u ON c.instructor_id = u.user_id
                WHERE e.user_id = ? AND e.status = 'PENDING'
                ORDER BY e.enrollment_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder message = new StringBuilder("Your pending course enrollments:\n\n");
            boolean hasPending = false;

            while (rs.next()) {
                hasPending = true;
                message.append("• ").append(rs.getString("course_name"))
                        .append(" (Instructor: ").append(rs.getString("instructor"))
                        .append(")\n  Requested: ").append(rs.getTimestamp("enrollment_date"))
                        .append("\n\n");
            }

            if (hasPending) {
                showAlert("Pending Enrollments", message.toString());
            } else {
                showAlert("Pending Enrollments", "You have no pending course enrollments.");
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load pending enrollments: " + e.getMessage());
        }
    }

    // ==================== NAVIGATION FUNCTIONS ====================

    @FXML
    private void showDashboard() {
        hideAllPanes();
        if (dashboardPane != null) {
            dashboardPane.setVisible(true);
            addFadeTransition(dashboardPane);
        }
    }

    private void hideAllPanes() {
        if (dashboardPane != null) dashboardPane.setVisible(false);
        if (myCoursesPane != null) myCoursesPane.setVisible(false);
        if (availableCoursesPane != null) availableCoursesPane.setVisible(false);
        if (courseMaterialsPane != null) courseMaterialsPane.setVisible(false);
        if (assignmentsPane != null) assignmentsPane.setVisible(false);
        if (submissionPane != null) submissionPane.setVisible(false);
        if (gradesPane != null) gradesPane.setVisible(false);
        if (messagesPane != null) messagesPane.setVisible(false);
    }

    private void addFadeTransition(VBox pane) {
        if (pane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), pane);
            fade.setFromValue(0.5);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    // ==================== UTILITY FUNCTIONS ====================

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/learningmanagement/learningmanagement/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) contentStackPane.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== MODEL CLASSES ====================

    public static class AvailableCourse {
        private final SimpleIntegerProperty courseId;
        private final SimpleStringProperty courseName;
        private final SimpleStringProperty instructor;
        private final SimpleIntegerProperty enrolledCount;

        public AvailableCourse(int courseId, String courseName, String instructor, int enrolledCount) {
            this.courseId = new SimpleIntegerProperty(courseId);
            this.courseName = new SimpleStringProperty(courseName);
            this.instructor = new SimpleStringProperty(instructor);
            this.enrolledCount = new SimpleIntegerProperty(enrolledCount);
        }

        public int getCourseId() { return courseId.get(); }
        public SimpleIntegerProperty courseIdProperty() { return courseId; }

        public String getCourseName() { return courseName.get(); }
        public SimpleStringProperty courseNameProperty() { return courseName; }

        public String getInstructor() { return instructor.get(); }
        public SimpleStringProperty instructorProperty() { return instructor; }

        public int getEnrolledCount() { return enrolledCount.get(); }
        public SimpleIntegerProperty enrolledCountProperty() { return enrolledCount; }
    }

    public static class CourseMaterial {
        private final SimpleStringProperty materialName;
        private final SimpleStringProperty materialType;
        private final SimpleStringProperty topicName;
        private final SimpleStringProperty uploadDate;

        public CourseMaterial(String materialName, String materialType, String topicName, String uploadDate) {
            this.materialName = new SimpleStringProperty(materialName);
            this.materialType = new SimpleStringProperty(materialType);
            this.topicName = new SimpleStringProperty(topicName);
            this.uploadDate = new SimpleStringProperty(uploadDate);
        }

        public String getMaterialName() { return materialName.get(); }
        public SimpleStringProperty materialNameProperty() { return materialName; }

        public String getMaterialType() { return materialType.get(); }
        public SimpleStringProperty materialTypeProperty() { return materialType; }

        public String getTopicName() { return topicName.get(); }
        public SimpleStringProperty topicNameProperty() { return topicName; }

        public String getUploadDate() { return uploadDate.get(); }
        public SimpleStringProperty uploadDateProperty() { return uploadDate; }
    }

    public static class StudentAssignment {
        private final SimpleIntegerProperty assignmentId;
        private final SimpleStringProperty assignmentName;
        private final SimpleStringProperty courseName;
        private final SimpleStringProperty dueDate;
        private final SimpleDoubleProperty maxPoints;
        private final SimpleStringProperty status;
        private final SimpleDoubleProperty grade;

        public StudentAssignment(int assignmentId, String assignmentName, String courseName, String dueDate, double maxPoints, String status, double grade) {
            this.assignmentId = new SimpleIntegerProperty(assignmentId);
            this.assignmentName = new SimpleStringProperty(assignmentName);
            this.courseName = new SimpleStringProperty(courseName);
            this.dueDate = new SimpleStringProperty(dueDate);
            this.maxPoints = new SimpleDoubleProperty(maxPoints);
            this.status = new SimpleStringProperty(status);
            this.grade = new SimpleDoubleProperty(grade);
        }

        public int getAssignmentId() { return assignmentId.get(); }
        public SimpleIntegerProperty assignmentIdProperty() { return assignmentId; }

        public String getAssignmentName() { return assignmentName.get(); }
        public SimpleStringProperty assignmentNameProperty() { return assignmentName; }

        public String getCourseName() { return courseName.get(); }
        public SimpleStringProperty courseNameProperty() { return courseName; }

        public String getDueDate() { return dueDate.get(); }
        public SimpleStringProperty dueDateProperty() { return dueDate; }

        public double getMaxPoints() { return maxPoints.get(); }
        public SimpleDoubleProperty maxPointsProperty() { return maxPoints; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }

        public double getGrade() { return grade.get(); }
        public SimpleDoubleProperty gradeProperty() { return grade; }
    }

    public static class StudentGrade {
        private final SimpleStringProperty assignmentName;
        private final SimpleStringProperty courseName;
        private final SimpleDoubleProperty grade;
        private final SimpleDoubleProperty maxPoints;
        private final SimpleStringProperty percentage;
        private final SimpleStringProperty feedback;

        public StudentGrade(String assignmentName, String courseName, double grade, double maxPoints, String percentage, String feedback) {
            this.assignmentName = new SimpleStringProperty(assignmentName);
            this.courseName = new SimpleStringProperty(courseName);
            this.grade = new SimpleDoubleProperty(grade);
            this.maxPoints = new SimpleDoubleProperty(maxPoints);
            this.percentage = new SimpleStringProperty(percentage);
            this.feedback = new SimpleStringProperty(feedback);
        }

        public String getAssignmentName() { return assignmentName.get(); }
        public SimpleStringProperty assignmentNameProperty() { return assignmentName; }

        public String getCourseName() { return courseName.get(); }
        public SimpleStringProperty courseNameProperty() { return courseName; }

        public double getGrade() { return grade.get(); }
        public SimpleDoubleProperty gradeProperty() { return grade; }

        public double getMaxPoints() { return maxPoints.get(); }
        public SimpleDoubleProperty maxPointsProperty() { return maxPoints; }

        public String getPercentage() { return percentage.get(); }
        public SimpleStringProperty percentageProperty() { return percentage; }

        public String getFeedback() { return feedback.get(); }
        public SimpleStringProperty feedbackProperty() { return feedback; }
    }

    public static class StudentMessage {
        private final SimpleIntegerProperty messageId;
        private final SimpleStringProperty messageType;
        private final SimpleStringProperty sender;
        private final SimpleStringProperty subject;
        private final SimpleStringProperty messageText;
        private final SimpleStringProperty sentDate;
        private final SimpleStringProperty readStatus;

        public StudentMessage(int messageId, String messageType, String sender, String subject, String messageText, String sentDate, String readStatus) {
            this.messageId = new SimpleIntegerProperty(messageId);
            this.messageType = new SimpleStringProperty(messageType);
            this.sender = new SimpleStringProperty(sender);
            this.subject = new SimpleStringProperty(subject);
            this.messageText = new SimpleStringProperty(messageText);
            this.sentDate = new SimpleStringProperty(sentDate);
            this.readStatus = new SimpleStringProperty(readStatus);
        }

        public int getMessageId() { return messageId.get(); }
        public SimpleIntegerProperty messageIdProperty() { return messageId; }

        public String getMessageType() { return messageType.get(); }
        public SimpleStringProperty messageTypeProperty() { return messageType; }

        public String getSender() { return sender.get(); }
        public SimpleStringProperty senderProperty() { return sender; }

        public String getSubject() { return subject.get(); }
        public SimpleStringProperty subjectProperty() { return subject; }

        public String getMessageText() { return messageText.get(); }
        public SimpleStringProperty messageTextProperty() { return messageText; }

        public String getSentDate() { return sentDate.get(); }
        public SimpleStringProperty sentDateProperty() { return sentDate; }

        public String getReadStatus() { return readStatus.get(); }
        public SimpleStringProperty readStatusProperty() { return readStatus; }
    }
}