module com.learningmanagement.learningmanagement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;

    opens com.learningmanagement.learningmanagement to javafx.fxml;
    opens com.learningmanagement.learningmanagement.controllers to javafx.fxml;

    exports com.learningmanagement.learningmanagement;
    exports com.learningmanagement.learningmanagement.controllers;
}