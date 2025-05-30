# Learning Management System (LMS)

A comprehensive desktop-based Learning Management System built with JavaFX and PostgreSQL.

## Features

### Core Functionality
- **User Authentication**: Role-based login system (Admin, Instructor, Student)
- **Course Management**: Create, update, delete, and enroll in courses
- **Student Management**: Comprehensive student information management
- **Instructor Management**: Instructor profile and course assignment
- **Progress Tracking**: Visual progress indicators and reporting

### JavaFX Features Implemented
- **Menu Bar & Menu Items**: Complete navigation system with keyboard shortcuts
- **Pagination & ScrollPane**: Displays 20+ items with smooth pagination
- **Progress Indicators**: Progress bars and indicators with animations
- **Visual Effects**: DropShadow and FadeTransition effects throughout the UI

### Technical Features
- **Database Integration**: PostgreSQL with comprehensive schema
- **Role-based Access Control**: Different interfaces for different user types
- **Responsive Design**: CSS-styled interface with modern look
- **Error Handling**: Comprehensive error handling and user feedback

## Prerequisites

- Java 11 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

## Setup Instructions

### 1. Database Setup

1. Install PostgreSQL and create a database named `lms_database`
2. Update database credentials in `DBUtil.java`:
   \`\`\`java
   private static final String URL = "jdbc:postgresql://localhost:5432/lms_database";
   private static final String USERNAME = "your_username";
   private static final String PASSWORD = "your_password";
   \`\`\`
3. Run the schema script:
   \`\`\`bash
   psql -U your_username -d lms_database -f database/schema.sql
   \`\`\`

### 2. Application Setup

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   \`\`\`bash
   mvn clean compile
   \`\`\`
4. Run the application:
   \`\`\`bash
   mvn javafx:run
   \`\`\`

## Default Login Credentials

- **Admin**: username: `admin`, password: `admin123`
- **Instructor**: username: `instructor1`, password: `inst123`
- **Student**: username: `student1`, password: `stud123`

## Project Structure

\`\`\`
src/
├── main/
│   ├── java/
│   │   └── com/learningmanagement/learningmanagementsystem/
│   │       ├── LMSApplication.java
│   │       ├── controllers/
│   │       ├── models/
│   │       └── utils/
│   └── resources/
│       ├── fxml/
│       └── css/
├── database/
│   └── schema.sql
└── pom.xml
\`\`\`

## Usage

### For Administrators
- Manage users, courses, students, and instructors
- View comprehensive reports and analytics
- Access all system features

### For Instructors
- Manage assigned courses
- View and manage enrolled students
- Track student progress

### For Students
- View available courses
- Enroll in courses
- Track personal progress

## Key Components

### Controllers
- `MainApplicationController`: Main window with menu bar
- `LoginController`: Authentication with visual effects
- `AdminDashboardController`: Admin-specific functionality
- `CourseController`: Course management
- `PaginatedViewController`: Demonstrates pagination and ScrollPane
- `ProgressViewController`: Progress tracking with indicators

### Models
- `User`: User authentication and roles
- `Student`: Student information
- `Instructor`: Instructor details
- `Course`: Course information
- `Assignment`: Assignment management

### Database Schema
- Comprehensive relational schema
- Foreign key constraints
- Sample data included

## Building Executable JAR

\`\`\`bash
mvn clean package
\`\`\`

The executable JAR will be created in the `target/` directory.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the repository.
