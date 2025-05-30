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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class InstructorDashboardController {

    // Main UI Components
    @FXML private StackPane contentStackPane;
    @FXML private VBox dashboardPane;
    @FXML private VBox materialManagementPane;
    @FXML private VBox topicManagementPane;
    @FXML private VBox assignmentManagementPane;
    @FXML private VBox studentMonitoringPane;
    @FXML private VBox messagingPane;

    // Dashboard components
    @FXML private Label currentInstructorLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label pendingGradingLabel;
    @FXML private ComboBox<String> courseSelectionCombo;

    // Material Management Components
    @FXML private TableView<CourseMaterial> materialTable;
    @FXML private TableColumn<CourseMaterial, Number> materialIdColumn;
    @FXML private TableColumn<CourseMaterial, String> materialNameColumn;
    @FXML private TableColumn<CourseMaterial, String> materialTypeColumn;
    @FXML private TableColumn<CourseMaterial, String> materialTopicColumn;
    @FXML private TableColumn<CourseMaterial, String> materialDateColumn;
    @FXML private ComboBox<String> materialTopicFilter;

    // Topic Management Components
    @FXML private TableView<CourseTopic> topicTable;
    @FXML private TableColumn<CourseTopic, Number> topicIdColumn;
    @FXML private TableColumn<CourseTopic, String> topicNameColumn;
    @FXML private TableColumn<CourseTopic, Number> topicOrderColumn;
    @FXML private TableColumn<CourseTopic, String> topicDescriptionColumn;

    // Assignment Management Components
    @FXML private TableView<Assignment> assignmentTable;
    @FXML private TableColumn<Assignment, Number> assignmentIdColumn;
    @FXML private TableColumn<Assignment, String> assignmentNameColumn;
    @FXML private TableColumn<Assignment, String> assignmentDueDateColumn;
    @FXML private TableColumn<Assignment, Number> assignmentMaxPointsColumn;
    @FXML private TableColumn<Assignment, Number> assignmentSubmissionsColumn;

    // Student Monitoring Components
    @FXML private TableView<StudentProgress> studentTable;
    @FXML private TableColumn<StudentProgress, Number> studentIdColumn;
    @FXML private TableColumn<StudentProgress, String> studentNameColumn;
    @FXML private TableColumn<StudentProgress, Number> studentProgressColumn;
    @FXML private TableColumn<StudentProgress, Number> studentGradeColumn;
    @FXML private TableColumn<StudentProgress, String> studentLastActiveColumn;

    // Messaging Components
    @FXML private ComboBox<String> messageRecipientCombo;
    @FXML private CheckBox sendToAllCheckBox;
    @FXML private TextField messageSubjectField;
    @FXML private TextArea messageTextArea;

    // Button references
    @FXML private Button logoutButton;

    private ObservableList<CourseMaterial> allMaterials = FXCollections.observableArrayList();
    private ObservableList<CourseTopic> allTopics = FXCollections.observableArrayList();
    private ObservableList<Assignment> allAssignments = FXCollections.observableArrayList();
    private ObservableList<StudentProgress> allStudents = FXCollections.observableArrayList();

    private int currentInstructorId = 2; // This should be set from login
    private int selectedCourseId = -1;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCourses();
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
        // Material table columns
        materialIdColumn.setCellValueFactory(cellData -> cellData.getValue().materialIdProperty());
        materialNameColumn.setCellValueFactory(cellData -> cellData.getValue().materialNameProperty());
        materialTypeColumn.setCellValueFactory(cellData -> cellData.getValue().materialTypeProperty());
        materialTopicColumn.setCellValueFactory(cellData -> cellData.getValue().topicNameProperty());
        materialDateColumn.setCellValueFactory(cellData -> cellData.getValue().uploadDateProperty());

        // Topic table columns
        topicIdColumn.setCellValueFactory(cellData -> cellData.getValue().topicIdProperty());
        topicNameColumn.setCellValueFactory(cellData -> cellData.getValue().topicNameProperty());
        topicOrderColumn.setCellValueFactory(cellData -> cellData.getValue().topicOrderProperty());
        topicDescriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());

        // Assignment table columns
        assignmentIdColumn.setCellValueFactory(cellData -> cellData.getValue().assignmentIdProperty());
        assignmentNameColumn.setCellValueFactory(cellData -> cellData.getValue().assignmentNameProperty());
        assignmentDueDateColumn.setCellValueFactory(cellData -> cellData.getValue().dueDateProperty());
        assignmentMaxPointsColumn.setCellValueFactory(cellData -> cellData.getValue().maxPointsProperty());
        assignmentSubmissionsColumn.setCellValueFactory(cellData -> cellData.getValue().submissionCountProperty());

        // Student table columns
        studentIdColumn.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        studentNameColumn.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        studentProgressColumn.setCellValueFactory(cellData -> cellData.getValue().progressProperty());
        studentGradeColumn.setCellValueFactory(cellData -> cellData.getValue().averageGradeProperty());
        studentLastActiveColumn.setCellValueFactory(cellData -> cellData.getValue().lastActiveProperty());
    }

    private void loadCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT course_id, course_name FROM courses WHERE instructor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentInstructorId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> courses = FXCollections.observableArrayList();
            while (rs.next()) {
                courses.add(rs.getInt("course_id") + " - " + rs.getString("course_name"));
            }
            courseSelectionCombo.setItems(courses);

            if (!courses.isEmpty()) {
                courseSelectionCombo.setValue(courses.get(0));
                onCourseSelected();
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    @FXML
    private void onCourseSelected() {
        String selected = courseSelectionCombo.getValue();
        if (selected != null) {
            selectedCourseId = Integer.parseInt(selected.split(" - ")[0]);
            loadInitialData();
        }
    }

    private void loadInitialData() {
        if (selectedCourseId != -1) {
            loadMaterials();
            loadTopics();
            loadAssignments();
            loadStudents();
            loadMessageRecipients();
        }
    }

    // ==================== MATERIAL MANAGEMENT ====================

    private void loadMaterials() {
        allMaterials.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT cm.material_id, cm.material_name, cm.material_type, 
                       cm.upload_date, ct.topic_name
                FROM course_materials cm
                LEFT JOIN course_topics ct ON cm.topic_id = ct.topic_id
                WHERE cm.course_id = ?
                ORDER BY cm.upload_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allMaterials.add(new CourseMaterial(
                        rs.getInt("material_id"),
                        rs.getString("material_name"),
                        rs.getString("material_type"),
                        rs.getString("topic_name") != null ? rs.getString("topic_name") : "No Topic",
                        rs.getTimestamp("upload_date").toString()
                ));
            }
            materialTable.setItems(allMaterials);

            // Load topic filter
            loadTopicFilter();
        } catch (SQLException e) {
            showAlert("Error", "Failed to load materials: " + e.getMessage());
        }
    }

    private void loadTopicFilter() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT topic_name FROM course_topics WHERE course_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> topics = FXCollections.observableArrayList();
            topics.add("All Topics");
            while (rs.next()) {
                topics.add(rs.getString("topic_name"));
            }
            materialTopicFilter.setItems(topics);
            materialTopicFilter.setValue("All Topics");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void uploadMaterial() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Material File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov"),
                new FileChooser.ExtensionFilter("Document Files", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(materialTable.getScene().getWindow());
        if (selectedFile != null) {
            showUploadMaterialDialog(selectedFile);
        }
    }

    private void showUploadMaterialDialog(File file) {
        Dialog<CourseMaterial> dialog = new Dialog<>();
        dialog.setTitle("Upload Material");
        dialog.setHeaderText("Upload: " + file.getName());

        ButtonType uploadButtonType = new ButtonType("Upload", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(uploadButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField materialName = new TextField(file.getName());
        ComboBox<String> materialType = new ComboBox<>();
        materialType.setItems(FXCollections.observableArrayList("PDF", "VIDEO", "NOTES", "DOCUMENT", "OTHER"));

        String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf": materialType.setValue("PDF"); break;
            case "mp4": case "avi": case "mov": materialType.setValue("VIDEO"); break;
            case "doc": case "docx": case "txt": materialType.setValue("DOCUMENT"); break;
            default: materialType.setValue("OTHER"); break;
        }

        ComboBox<String> topicCombo = new ComboBox<>();
        loadTopicsForCombo(topicCombo);

        grid.add(new Label("Material Name:"), 0, 0);
        grid.add(materialName, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(materialType, 1, 1);
        grid.add(new Label("Topic:"), 0, 2);
        grid.add(topicCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == uploadButtonType) {
                try {
                    // Create uploads directory if it doesn't exist
                    Path uploadsDir = Paths.get("uploads");
                    if (!Files.exists(uploadsDir)) {
                        Files.createDirectories(uploadsDir);
                    }

                    // Copy file to uploads directory
                    String fileName = System.currentTimeMillis() + "_" + file.getName();
                    Path targetPath = uploadsDir.resolve(fileName);
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                    // Save to database
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "INSERT INTO course_materials (course_id, material_name, material_type, file_path, topic_id) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, selectedCourseId);
                        stmt.setString(2, materialName.getText());
                        stmt.setString(3, materialType.getValue());
                        stmt.setString(4, targetPath.toString());

                        if (topicCombo.getValue() != null && !topicCombo.getValue().equals("No Topic")) {
                            int topicId = getTopicId(topicCombo.getValue());
                            stmt.setInt(5, topicId);
                        } else {
                            stmt.setNull(5, Types.INTEGER);
                        }

                        stmt.executeUpdate();
                        loadMaterials();
                        showAlert("Success", "Material uploaded successfully!");
                    }
                } catch (Exception e) {
                    showAlert("Error", "Failed to upload material: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void updateMaterial() {
        CourseMaterial selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a material to update.");
            return;
        }

        Dialog<CourseMaterial> dialog = new Dialog<>();
        dialog.setTitle("Update Material");
        dialog.setHeaderText("Update: " + selected.getMaterialName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField materialName = new TextField(selected.getMaterialName());
        ComboBox<String> materialType = new ComboBox<>();
        materialType.setItems(FXCollections.observableArrayList("PDF", "VIDEO", "NOTES", "DOCUMENT", "OTHER"));
        materialType.setValue(selected.getMaterialType());

        ComboBox<String> topicCombo = new ComboBox<>();
        loadTopicsForCombo(topicCombo);
        topicCombo.setValue(selected.getTopicName());

        grid.add(new Label("Material Name:"), 0, 0);
        grid.add(materialName, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(materialType, 1, 1);
        grid.add(new Label("Topic:"), 0, 2);
        grid.add(topicCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE course_materials SET material_name = ?, material_type = ?, topic_id = ? WHERE material_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, materialName.getText());
                    stmt.setString(2, materialType.getValue());

                    if (topicCombo.getValue() != null && !topicCombo.getValue().equals("No Topic")) {
                        int topicId = getTopicId(topicCombo.getValue());
                        stmt.setInt(3, topicId);
                    } else {
                        stmt.setNull(3, Types.INTEGER);
                    }

                    stmt.setInt(4, selected.getMaterialId());
                    stmt.executeUpdate();

                    loadMaterials();
                    showAlert("Success", "Material updated successfully!");
                } catch (SQLException e) {
                    showAlert("Error", "Failed to update material: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void deleteMaterial() {
        CourseMaterial selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a material to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Material");
        confirmAlert.setContentText("Are you sure you want to delete: " + selected.getMaterialName() + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM course_materials WHERE material_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selected.getMaterialId());
                stmt.executeUpdate();

                loadMaterials();
                showAlert("Success", "Material deleted successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete material: " + e.getMessage());
            }
        }
    }

    @FXML
    private void filterMaterials() {
        String selectedTopic = materialTopicFilter.getValue();
        if (selectedTopic == null || selectedTopic.equals("All Topics")) {
            materialTable.setItems(allMaterials);
        } else {
            ObservableList<CourseMaterial> filtered = FXCollections.observableArrayList();
            for (CourseMaterial material : allMaterials) {
                if (material.getTopicName().equals(selectedTopic)) {
                    filtered.add(material);
                }
            }
            materialTable.setItems(filtered);
        }
    }

    // ==================== TOPIC MANAGEMENT ====================

    private void loadTopics() {
        allTopics.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT topic_id, topic_name, topic_order, description FROM course_topics WHERE course_id = ? ORDER BY topic_order";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allTopics.add(new CourseTopic(
                        rs.getInt("topic_id"),
                        rs.getString("topic_name"),
                        rs.getInt("topic_order"),
                        rs.getString("description")
                ));
            }
            topicTable.setItems(allTopics);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load topics: " + e.getMessage());
        }
    }

    @FXML
    private void createTopic() {
        Dialog<CourseTopic> dialog = new Dialog<>();
        dialog.setTitle("Create Topic");
        dialog.setHeaderText("Create new topic/unit");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField topicName = new TextField();
        TextField topicOrder = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(3);

        // Get next order number
        int nextOrder = allTopics.size() + 1;
        topicOrder.setText(String.valueOf(nextOrder));

        grid.add(new Label("Topic Name:"), 0, 0);
        grid.add(topicName, 1, 0);
        grid.add(new Label("Order:"), 0, 1);
        grid.add(topicOrder, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(description, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO course_topics (course_id, topic_name, topic_order, description) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, selectedCourseId);
                    stmt.setString(2, topicName.getText());
                    stmt.setInt(3, Integer.parseInt(topicOrder.getText()));
                    stmt.setString(4, description.getText());

                    stmt.executeUpdate();
                    loadTopics();
                    loadTopicFilter(); // Refresh material topic filter
                    showAlert("Success", "Topic created successfully!");
                } catch (SQLException | NumberFormatException e) {
                    showAlert("Error", "Failed to create topic: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void updateTopic() {
        CourseTopic selected = topicTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a topic to update.");
            return;
        }

        Dialog<CourseTopic> dialog = new Dialog<>();
        dialog.setTitle("Update Topic");
        dialog.setHeaderText("Update: " + selected.getTopicName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField topicName = new TextField(selected.getTopicName());
        TextField topicOrder = new TextField(String.valueOf(selected.getTopicOrder()));
        TextArea description = new TextArea(selected.getDescription());
        description.setPrefRowCount(3);

        grid.add(new Label("Topic Name:"), 0, 0);
        grid.add(topicName, 1, 0);
        grid.add(new Label("Order:"), 0, 1);
        grid.add(topicOrder, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(description, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE course_topics SET topic_name = ?, topic_order = ?, description = ? WHERE topic_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, topicName.getText());
                    stmt.setInt(2, Integer.parseInt(topicOrder.getText()));
                    stmt.setString(3, description.getText());
                    stmt.setInt(4, selected.getTopicId());

                    stmt.executeUpdate();
                    loadTopics();
                    loadTopicFilter(); // Refresh material topic filter
                    showAlert("Success", "Topic updated successfully!");
                } catch (SQLException | NumberFormatException e) {
                    showAlert("Error", "Failed to update topic: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void deleteTopic() {
        CourseTopic selected = topicTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a topic to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Topic");
        confirmAlert.setContentText("Are you sure you want to delete: " + selected.getTopicName() + "?\nThis will also remove topic association from materials.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First update materials to remove topic association
                String updateMaterials = "UPDATE course_materials SET topic_id = NULL WHERE topic_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateMaterials);
                updateStmt.setInt(1, selected.getTopicId());
                updateStmt.executeUpdate();

                // Then delete the topic
                String sql = "DELETE FROM course_topics WHERE topic_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selected.getTopicId());
                stmt.executeUpdate();

                loadTopics();
                loadMaterials(); // Refresh materials
                loadTopicFilter(); // Refresh material topic filter
                showAlert("Success", "Topic deleted successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete topic: " + e.getMessage());
            }
        }
    }

    // ==================== ASSIGNMENT MANAGEMENT ====================

    private void loadAssignments() {
        allAssignments.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.assignment_id, a.assignment_name, a.due_date, a.max_points,
                       COUNT(s.submission_id) as submission_count
                FROM assignments a
                LEFT JOIN assignment_submissions s ON a.assignment_id = s.assignment_id
                WHERE a.course_id = ?
                GROUP BY a.assignment_id, a.assignment_name, a.due_date, a.max_points
                ORDER BY a.due_date
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String dueDate = rs.getTimestamp("due_date") != null ?
                        rs.getTimestamp("due_date").toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                        "No due date";

                allAssignments.add(new Assignment(
                        rs.getInt("assignment_id"),
                        rs.getString("assignment_name"),
                        dueDate,
                        rs.getDouble("max_points"),
                        rs.getInt("submission_count")
                ));
            }
            assignmentTable.setItems(allAssignments);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load assignments: " + e.getMessage());
        }
    }

    @FXML
    private void createAssignment() {
        Dialog<Assignment> dialog = new Dialog<>();
        dialog.setTitle("Create Assignment");
        dialog.setHeaderText("Create new assignment");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField assignmentName = new TextField();
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        DatePicker dueDatePicker = new DatePicker();
        TextField dueTime = new TextField("23:59");
        TextField maxPoints = new TextField();

        grid.add(new Label("Assignment Name:"), 0, 0);
        grid.add(assignmentName, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(description, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDatePicker, 1, 2);
        grid.add(new Label("Due Time (HH:MM):"), 0, 3);
        grid.add(dueTime, 1, 3);
        grid.add(new Label("Max Points:"), 0, 4);
        grid.add(maxPoints, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO assignments (course_id, assignment_name, description, due_date, max_points) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, selectedCourseId);
                    stmt.setString(2, assignmentName.getText());
                    stmt.setString(3, description.getText());

                    if (dueDatePicker.getValue() != null) {
                        String dateTimeStr = dueDatePicker.getValue() + " " + dueTime.getText() + ":00";
                        stmt.setTimestamp(4, Timestamp.valueOf(dateTimeStr));
                    } else {
                        stmt.setNull(4, Types.TIMESTAMP);
                    }

                    stmt.setDouble(5, Double.parseDouble(maxPoints.getText()));

                    stmt.executeUpdate();
                    loadAssignments();
                    updateQuickStats();
                    showAlert("Success", "Assignment created successfully!");
                } catch (SQLException | NumberFormatException e) {
                    showAlert("Error", "Failed to create assignment: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void updateAssignment() {
        Assignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an assignment to update.");
            return;
        }

        // Implementation similar to createAssignment but with pre-filled values
        showAlert("Info", "Update assignment functionality - implementation similar to create");
    }

    @FXML
    private void deleteAssignment() {
        Assignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an assignment to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Assignment");
        confirmAlert.setContentText("Are you sure you want to delete: " + selected.getAssignmentName() + "?\nThis will also delete all submissions.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // First delete submissions
                String deleteSubmissions = "DELETE FROM assignment_submissions WHERE assignment_id = ?";
                PreparedStatement submissionStmt = conn.prepareStatement(deleteSubmissions);
                submissionStmt.setInt(1, selected.getAssignmentId());
                submissionStmt.executeUpdate();

                // Then delete assignment
                String sql = "DELETE FROM assignments WHERE assignment_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, selected.getAssignmentId());
                stmt.executeUpdate();

                loadAssignments();
                updateQuickStats();
                showAlert("Success", "Assignment deleted successfully!");
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete assignment: " + e.getMessage());
            }
        }
    }

    @FXML
    private void viewSubmissions() {
        Assignment selected = assignmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an assignment to view submissions.");
            return;
        }

        showSubmissionsDialog(selected);
    }

    private void showSubmissionsDialog(Assignment assignment) {
        Stage submissionStage = new Stage();
        submissionStage.setTitle("Submissions - " + assignment.getAssignmentName());
        submissionStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("Assignment Submissions");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Table for submissions
        TableView<AssignmentSubmission> submissionTable = new TableView<>();
        TableColumn<AssignmentSubmission, String> studentCol = new TableColumn<>("Student");
        TableColumn<AssignmentSubmission, String> submissionDateCol = new TableColumn<>("Submission Date");
        TableColumn<AssignmentSubmission, Number> gradeCol = new TableColumn<>("Grade");
        TableColumn<AssignmentSubmission, String> feedbackCol = new TableColumn<>("Feedback");

        studentCol.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        submissionDateCol.setCellValueFactory(cellData -> cellData.getValue().submissionDateProperty());
        gradeCol.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        feedbackCol.setCellValueFactory(cellData -> cellData.getValue().feedbackProperty());

        submissionTable.getColumns().addAll(studentCol, submissionDateCol, gradeCol, feedbackCol);

        // Load submissions
        ObservableList<AssignmentSubmission> submissions = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT s.submission_id, u.username, s.submission_date, s.grade, s.feedback
                FROM assignment_submissions s
                JOIN users u ON s.student_id = u.user_id
                WHERE s.assignment_id = ?
                ORDER BY s.submission_date DESC
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, assignment.getAssignmentId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                submissions.add(new AssignmentSubmission(
                        rs.getInt("submission_id"),
                        rs.getString("username"),
                        rs.getTimestamp("submission_date").toString(),
                        rs.getDouble("grade"),
                        rs.getString("feedback") != null ? rs.getString("feedback") : "No feedback"
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load submissions: " + e.getMessage());
        }

        submissionTable.setItems(submissions);

        // Buttons
        HBox buttonBox = new HBox(10);
        Button gradeButton = new Button("Grade Submission");
        Button viewDetailsButton = new Button("View Details");

        gradeButton.setOnAction(e -> {
            AssignmentSubmission selected = submissionTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showGradingDialog(selected, assignment.getMaxPoints());
                // Reload submissions after grading
                submissionTable.setItems(submissions);
            }
        });

        buttonBox.getChildren().addAll(gradeButton, viewDetailsButton);

        root.getChildren().addAll(title, submissionTable, buttonBox);

        Scene scene = new Scene(root, 800, 500);
        submissionStage.setScene(scene);
        submissionStage.showAndWait();
    }

    private void showGradingDialog(AssignmentSubmission submission, double maxPoints) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Grade Submission");
        dialog.setHeaderText("Grade submission by: " + submission.getStudentName());

        ButtonType gradeButtonType = new ButtonType("Grade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(gradeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField gradeField = new TextField(String.valueOf(submission.getGrade()));
        TextArea feedbackArea = new TextArea(submission.getFeedback());
        feedbackArea.setPrefRowCount(4);

        grid.add(new Label("Grade (0-" + maxPoints + "):"), 0, 0);
        grid.add(gradeField, 1, 0);
        grid.add(new Label("Feedback:"), 0, 1);
        grid.add(feedbackArea, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == gradeButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "UPDATE assignment_submissions SET grade = ?, feedback = ?, graded_date = CURRENT_TIMESTAMP WHERE submission_id = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setDouble(1, Double.parseDouble(gradeField.getText()));
                    stmt.setString(2, feedbackArea.getText());
                    stmt.setInt(3, submission.getSubmissionId());
                    stmt.executeUpdate();

                    showAlert("Success", "Submission graded successfully!");
                } catch (SQLException | NumberFormatException e) {
                    showAlert("Error", "Failed to grade submission: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ==================== STUDENT MONITORING ====================

    private void loadStudents() {
        allStudents.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username, 
                       COALESCE(p.progress_percentage, 0) as progress,
                       COALESCE(AVG(s.grade), 0) as avg_grade
                FROM users u
                JOIN enrollments e ON u.user_id = e.user_id
                LEFT JOIN progress p ON u.user_id = p.user_id AND p.course_id = ?
                LEFT JOIN assignment_submissions s ON u.user_id = s.student_id
                LEFT JOIN assignments a ON s.assignment_id = a.assignment_id AND a.course_id = ?
                WHERE e.course_id = ?
                GROUP BY u.user_id, u.username, p.progress_percentage
                ORDER BY u.username
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            stmt.setInt(2, selectedCourseId);
            stmt.setInt(3, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                allStudents.add(new StudentProgress(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getDouble("progress"),
                        rs.getDouble("avg_grade"),
                        "Recently" // Placeholder for last active
                ));
            }
            studentTable.setItems(allStudents);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load students: " + e.getMessage());
        }
    }

    @FXML
    private void viewStudentDetails() {
        StudentProgress selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a student to view details.");
            return;
        }

        showStudentDetailsDialog(selected);
    }

    private void showStudentDetailsDialog(StudentProgress student) {
        Stage detailStage = new Stage();
        detailStage.setTitle("Student Details - " + student.getStudentName());
        detailStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Student Progress Details");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Student info
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);

        infoGrid.add(new Label("Student Name:"), 0, 0);
        infoGrid.add(new Label(student.getStudentName()), 1, 0);
        infoGrid.add(new Label("Overall Progress:"), 0, 1);
        infoGrid.add(new Label(String.format("%.1f%%", student.getProgress())), 1, 1);
        infoGrid.add(new Label("Average Grade:"), 0, 2);
        infoGrid.add(new Label(String.format("%.1f", student.getAverageGrade())), 1, 2);

        // Assignment grades table
        TableView<StudentAssignmentGrade> gradesTable = new TableView<>();
        TableColumn<StudentAssignmentGrade, String> assignmentCol = new TableColumn<>("Assignment");
        TableColumn<StudentAssignmentGrade, Number> gradeCol = new TableColumn<>("Grade");
        TableColumn<StudentAssignmentGrade, Number> maxPointsCol = new TableColumn<>("Max Points");
        TableColumn<StudentAssignmentGrade, String> feedbackCol = new TableColumn<>("Feedback");

        assignmentCol.setCellValueFactory(cellData -> cellData.getValue().assignmentNameProperty());
        gradeCol.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        maxPointsCol.setCellValueFactory(cellData -> cellData.getValue().maxPointsProperty());
        feedbackCol.setCellValueFactory(cellData -> cellData.getValue().feedbackProperty());

        gradesTable.getColumns().addAll(assignmentCol, gradeCol, maxPointsCol, feedbackCol);

        // Load student's assignment grades
        ObservableList<StudentAssignmentGrade> grades = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT a.assignment_name, s.grade, a.max_points, s.feedback
                FROM assignments a
                LEFT JOIN assignment_submissions s ON a.assignment_id = s.assignment_id AND s.student_id = ?
                WHERE a.course_id = ?
                ORDER BY a.assignment_name
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, student.getStudentId());
            stmt.setInt(2, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                grades.add(new StudentAssignmentGrade(
                        rs.getString("assignment_name"),
                        rs.getDouble("grade"),
                        rs.getDouble("max_points"),
                        rs.getString("feedback") != null ? rs.getString("feedback") : "No submission"
                ));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load student grades: " + e.getMessage());
        }

        gradesTable.setItems(grades);

        root.getChildren().addAll(title, infoGrid, new Label("Assignment Grades:"), gradesTable);

        Scene scene = new Scene(root, 700, 500);
        detailStage.setScene(scene);
        detailStage.showAndWait();
    }

    @FXML
    private void sendMessageToStudent() {
        StudentProgress selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a student to send a message.");
            return;
        }

        showSendMessageDialog(selected.getStudentId(), selected.getStudentName());
    }

    // ==================== MESSAGING ====================

    private void loadMessageRecipients() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = """
                SELECT u.user_id, u.username
                FROM users u
                JOIN enrollments e ON u.user_id = e.user_id
                WHERE e.course_id = ?
                ORDER BY u.username
                """;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> recipients = FXCollections.observableArrayList();
            while (rs.next()) {
                recipients.add(rs.getInt("user_id") + " - " + rs.getString("username"));
            }
            messageRecipientCombo.setItems(recipients);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void sendMessage() {
        if (sendToAllCheckBox.isSelected()) {
            sendMessageToAllStudents();
        } else {
            String recipient = messageRecipientCombo.getValue();
            if (recipient == null) {
                showAlert("Error", "Please select a recipient.");
                return;
            }
            int recipientId = Integer.parseInt(recipient.split(" - ")[0]);
            sendMessageToRecipient(recipientId);
        }
    }

    private void sendMessageToAllStudents() {
        String subject = messageSubjectField.getText();
        String messageText = messageTextArea.getText();

        if (subject.isEmpty() || messageText.isEmpty()) {
            showAlert("Error", "Please fill in subject and message.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String getStudentsSql = """
                SELECT u.user_id
                FROM users u
                JOIN enrollments e ON u.user_id = e.user_id
                WHERE e.course_id = ?
                """;
            PreparedStatement getStudentsStmt = conn.prepareStatement(getStudentsSql);
            getStudentsStmt.setInt(1, selectedCourseId);
            ResultSet rs = getStudentsStmt.executeQuery();

            String insertSql = "INSERT INTO messages (sender_id, recipient_id, course_id, subject, message_text, message_type) VALUES (?, ?, ?, ?, ?, 'ANNOUNCEMENT')";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);

            int messageCount = 0;
            while (rs.next()) {
                insertStmt.setInt(1, currentInstructorId);
                insertStmt.setInt(2, rs.getInt("user_id"));
                insertStmt.setInt(3, selectedCourseId);
                insertStmt.setString(4, subject);
                insertStmt.setString(5, messageText);
                insertStmt.executeUpdate();
                messageCount++;
            }

            messageSubjectField.clear();
            messageTextArea.clear();
            showAlert("Success", "Message sent to " + messageCount + " students!");
        } catch (SQLException e) {
            showAlert("Error", "Failed to send messages: " + e.getMessage());
        }
    }

    private void sendMessageToRecipient(int recipientId) {
        String subject = messageSubjectField.getText();
        String messageText = messageTextArea.getText();

        if (subject.isEmpty() || messageText.isEmpty()) {
            showAlert("Error", "Please fill in subject and message.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO messages (sender_id, recipient_id, course_id, subject, message_text) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentInstructorId);
            stmt.setInt(2, recipientId);
            stmt.setInt(3, selectedCourseId);
            stmt.setString(4, subject);
            stmt.setString(5, messageText);
            stmt.executeUpdate();

            messageSubjectField.clear();
            messageTextArea.clear();
            showAlert("Success", "Message sent successfully!");
        } catch (SQLException e) {
            showAlert("Error", "Failed to send message: " + e.getMessage());
        }
    }

    private void showSendMessageDialog(int recipientId, String recipientName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Send Message");
        dialog.setHeaderText("Send message to: " + recipientName);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField subject = new TextField();
        TextArea messageText = new TextArea();
        messageText.setPrefRowCount(5);

        grid.add(new Label("Subject:"), 0, 0);
        grid.add(subject, 1, 0);
        grid.add(new Label("Message:"), 0, 1);
        grid.add(messageText, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO messages (sender_id, recipient_id, course_id, subject, message_text) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, currentInstructorId);
                    stmt.setInt(2, recipientId);
                    stmt.setInt(3, selectedCourseId);
                    stmt.setString(4, subject.getText());
                    stmt.setString(5, messageText.getText());
                    stmt.executeUpdate();

                    showAlert("Success", "Message sent successfully!");
                } catch (SQLException e) {
                    showAlert("Error", "Failed to send message: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void showGrading() {
        showAlert("Info", "Grading functionality is available through Assignment Management -> View Submissions");
    }

    // ==================== NAVIGATION FUNCTIONS ====================

    @FXML
    private void showMaterialManagement() {
        hideAllPanes();
        materialManagementPane.setVisible(true);
        loadMaterials();
        addFadeTransition(materialManagementPane);
    }

    @FXML
    private void showTopicManagement() {
        hideAllPanes();
        topicManagementPane.setVisible(true);
        loadTopics();
        addFadeTransition(topicManagementPane);
    }

    @FXML
    private void showAssignmentManagement() {
        hideAllPanes();
        assignmentManagementPane.setVisible(true);
        loadAssignments();
        addFadeTransition(assignmentManagementPane);
    }

    @FXML
    private void showStudentMonitoring() {
        hideAllPanes();
        studentMonitoringPane.setVisible(true);
        loadStudents();
        addFadeTransition(studentMonitoringPane);
    }

    @FXML
    private void showMessaging() {
        hideAllPanes();
        messagingPane.setVisible(true);
        loadMessageRecipients();
        addFadeTransition(messagingPane);
    }

    private void showDashboard() {
        hideAllPanes();
        dashboardPane.setVisible(true);
        addFadeTransition(dashboardPane);
    }

    private void hideAllPanes() {
        dashboardPane.setVisible(false);
        materialManagementPane.setVisible(false);
        topicManagementPane.setVisible(false);
        assignmentManagementPane.setVisible(false);
        studentMonitoringPane.setVisible(false);
        messagingPane.setVisible(false);
    }

    // ==================== UTILITY FUNCTIONS ====================

    private void updateQuickStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total courses for instructor
            String courseCountSql = "SELECT COUNT(*) FROM courses WHERE instructor_id = ?";
            PreparedStatement courseStmt = conn.prepareStatement(courseCountSql);
            courseStmt.setInt(1, currentInstructorId);
            ResultSet courseRs = courseStmt.executeQuery();
            if (courseRs.next()) {
                totalCoursesLabel.setText("My Courses: " + courseRs.getInt(1));
            }

            // Total students across all instructor's courses
            String studentCountSql = """
                SELECT COUNT(DISTINCT e.user_id) 
                FROM enrollments e 
                JOIN courses c ON e.course_id = c.course_id 
                WHERE c.instructor_id = ?
                """;
            PreparedStatement studentStmt = conn.prepareStatement(studentCountSql);
            studentStmt.setInt(1, currentInstructorId);
            ResultSet studentRs = studentStmt.executeQuery();
            if (studentRs.next()) {
                totalStudentsLabel.setText("Total Students: " + studentRs.getInt(1));
            }

            // Pending grading (submissions without grades)
            String pendingGradingSql = """
                SELECT COUNT(*) 
                FROM assignment_submissions s 
                JOIN assignments a ON s.assignment_id = a.assignment_id 
                JOIN courses c ON a.course_id = c.course_id 
                WHERE c.instructor_id = ? AND s.grade IS NULL
                """;
            PreparedStatement pendingStmt = conn.prepareStatement(pendingGradingSql);
            pendingStmt.setInt(1, currentInstructorId);
            ResultSet pendingRs = pendingStmt.executeQuery();
            if (pendingRs.next()) {
                pendingGradingLabel.setText("Pending Grading: " + pendingRs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        sendToAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            messageRecipientCombo.setDisable(newVal);
        });
    }

    private void loadTopicsForCombo(ComboBox<String> combo) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT topic_name FROM course_topics WHERE course_id = ? ORDER BY topic_order";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedCourseId);
            ResultSet rs = stmt.executeQuery();

            ObservableList<String> topics = FXCollections.observableArrayList();
            topics.add("No Topic");
            while (rs.next()) {
                topics.add(rs.getString("topic_name"));
            }
            combo.setItems(topics);
            combo.setValue("No Topic");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getTopicId(String topicName) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT topic_id FROM course_topics WHERE topic_name = ? AND course_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, topicName);
            stmt.setInt(2, selectedCourseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("topic_id");
            }
        }
        throw new SQLException("Topic not found: " + topicName);
    }

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

    private void addButtonEffects() {
        Button[] buttons = {logoutButton};

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

    public static class CourseMaterial {
        private final SimpleIntegerProperty materialId;
        private final SimpleStringProperty materialName;
        private final SimpleStringProperty materialType;
        private final SimpleStringProperty topicName;
        private final SimpleStringProperty uploadDate;

        public CourseMaterial(int materialId, String materialName, String materialType, String topicName, String uploadDate) {
            this.materialId = new SimpleIntegerProperty(materialId);
            this.materialName = new SimpleStringProperty(materialName);
            this.materialType = new SimpleStringProperty(materialType);
            this.topicName = new SimpleStringProperty(topicName);
            this.uploadDate = new SimpleStringProperty(uploadDate);
        }

        public int getMaterialId() { return materialId.get(); }
        public SimpleIntegerProperty materialIdProperty() { return materialId; }

        public String getMaterialName() { return materialName.get(); }
        public SimpleStringProperty materialNameProperty() { return materialName; }

        public String getMaterialType() { return materialType.get(); }
        public SimpleStringProperty materialTypeProperty() { return materialType; }

        public String getTopicName() { return topicName.get(); }
        public SimpleStringProperty topicNameProperty() { return topicName; }

        public String getUploadDate() { return uploadDate.get(); }
        public SimpleStringProperty uploadDateProperty() { return uploadDate; }
    }

    public static class CourseTopic {
        private final SimpleIntegerProperty topicId;
        private final SimpleStringProperty topicName;
        private final SimpleIntegerProperty topicOrder;
        private final SimpleStringProperty description;

        public CourseTopic(int topicId, String topicName, int topicOrder, String description) {
            this.topicId = new SimpleIntegerProperty(topicId);
            this.topicName = new SimpleStringProperty(topicName);
            this.topicOrder = new SimpleIntegerProperty(topicOrder);
            this.description = new SimpleStringProperty(description);
        }

        public int getTopicId() { return topicId.get(); }
        public SimpleIntegerProperty topicIdProperty() { return topicId; }

        public String getTopicName() { return topicName.get(); }
        public SimpleStringProperty topicNameProperty() { return topicName; }

        public int getTopicOrder() { return topicOrder.get(); }
        public SimpleIntegerProperty topicOrderProperty() { return topicOrder; }

        public String getDescription() { return description.get(); }
        public SimpleStringProperty descriptionProperty() { return description; }
    }

    public static class Assignment {
        private final SimpleIntegerProperty assignmentId;
        private final SimpleStringProperty assignmentName;
        private final SimpleStringProperty dueDate;
        private final SimpleDoubleProperty maxPoints;
        private final SimpleIntegerProperty submissionCount;

        public Assignment(int assignmentId, String assignmentName, String dueDate, double maxPoints, int submissionCount) {
            this.assignmentId = new SimpleIntegerProperty(assignmentId);
            this.assignmentName = new SimpleStringProperty(assignmentName);
            this.dueDate = new SimpleStringProperty(dueDate);
            this.maxPoints = new SimpleDoubleProperty(maxPoints);
            this.submissionCount = new SimpleIntegerProperty(submissionCount);
        }

        public int getAssignmentId() { return assignmentId.get(); }
        public SimpleIntegerProperty assignmentIdProperty() { return assignmentId; }

        public String getAssignmentName() { return assignmentName.get(); }
        public SimpleStringProperty assignmentNameProperty() { return assignmentName; }

        public String getDueDate() { return dueDate.get(); }
        public SimpleStringProperty dueDateProperty() { return dueDate; }

        public double getMaxPoints() { return maxPoints.get(); }
        public SimpleDoubleProperty maxPointsProperty() { return maxPoints; }

        public int getSubmissionCount() { return submissionCount.get(); }
        public SimpleIntegerProperty submissionCountProperty() { return submissionCount; }
    }

    public static class StudentProgress {
        private final SimpleIntegerProperty studentId;
        private final SimpleStringProperty studentName;
        private final SimpleDoubleProperty progress;
        private final SimpleDoubleProperty averageGrade;
        private final SimpleStringProperty lastActive;

        public StudentProgress(int studentId, String studentName, double progress, double averageGrade, String lastActive) {
            this.studentId = new SimpleIntegerProperty(studentId);
            this.studentName = new SimpleStringProperty(studentName);
            this.progress = new SimpleDoubleProperty(progress);
            this.averageGrade = new SimpleDoubleProperty(averageGrade);
            this.lastActive = new SimpleStringProperty(lastActive);
        }

        public int getStudentId() { return studentId.get(); }
        public SimpleIntegerProperty studentIdProperty() { return studentId; }

        public String getStudentName() { return studentName.get(); }
        public SimpleStringProperty studentNameProperty() { return studentName; }

        public double getProgress() { return progress.get(); }
        public SimpleDoubleProperty progressProperty() { return progress; }

        public double getAverageGrade() { return averageGrade.get(); }
        public SimpleDoubleProperty averageGradeProperty() { return averageGrade; }

        public String getLastActive() { return lastActive.get(); }
        public SimpleStringProperty lastActiveProperty() { return lastActive; }
    }

    public static class AssignmentSubmission {
        private final SimpleIntegerProperty submissionId;
        private final SimpleStringProperty studentName;
        private final SimpleStringProperty submissionDate;
        private final SimpleDoubleProperty grade;
        private final SimpleStringProperty feedback;

        public AssignmentSubmission(int submissionId, String studentName, String submissionDate, double grade, String feedback) {
            this.submissionId = new SimpleIntegerProperty(submissionId);
            this.studentName = new SimpleStringProperty(studentName);
            this.submissionDate = new SimpleStringProperty(submissionDate);
            this.grade = new SimpleDoubleProperty(grade);
            this.feedback = new SimpleStringProperty(feedback);
        }

        public int getSubmissionId() { return submissionId.get(); }
        public SimpleIntegerProperty submissionIdProperty() { return submissionId; }

        public String getStudentName() { return studentName.get(); }
        public SimpleStringProperty studentNameProperty() { return studentName; }

        public String getSubmissionDate() { return submissionDate.get(); }
        public SimpleStringProperty submissionDateProperty() { return submissionDate; }

        public double getGrade() { return grade.get(); }
        public SimpleDoubleProperty gradeProperty() { return grade; }

        public String getFeedback() { return feedback.get(); }
        public SimpleStringProperty feedbackProperty() { return feedback; }
    }

    public static class StudentAssignmentGrade {
        private final SimpleStringProperty assignmentName;
        private final SimpleDoubleProperty grade;
        private final SimpleDoubleProperty maxPoints;
        private final SimpleStringProperty feedback;

        public StudentAssignmentGrade(String assignmentName, double grade, double maxPoints, String feedback) {
            this.assignmentName = new SimpleStringProperty(assignmentName);
            this.grade = new SimpleDoubleProperty(grade);
            this.maxPoints = new SimpleDoubleProperty(maxPoints);
            this.feedback = new SimpleStringProperty(feedback);
        }

        public String getAssignmentName() { return assignmentName.get(); }
        public SimpleStringProperty assignmentNameProperty() { return assignmentName; }

        public double getGrade() { return grade.get(); }
        public SimpleDoubleProperty gradeProperty() { return grade; }

        public double getMaxPoints() { return maxPoints.get(); }
        public SimpleDoubleProperty maxPointsProperty() { return maxPoints; }

        public String getFeedback() { return feedback.get(); }
        public SimpleStringProperty feedbackProperty() { return feedback; }
    }
}