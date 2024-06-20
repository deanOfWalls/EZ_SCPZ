# CRUD Template

This project is a Maven-based CRUD Template using Java, Spring Boot, and an H2 database. It's designed as a starting point for beginners learning Java, Spring Boot, and basic CRUD operations.

## Setup

### Option 1: Download as ZIP (Recommended for a Fresh Start)

1. Navigate to the GitHub repository page.
2. Look for the “Code” button and click it.
3. Choose "Download ZIP".
4. Extract the ZIP file in your desired location to start working on the project.

### Option 2: Clone the Repository (and Remove Git History)

1. Clone the repository.
2. Navigate to the project directory in the terminal.
3. Run `rm -rf .git` to remove the existing `.git` directory. This will delete the current Git history.
4. Initialize a new Git repository with `git init`, if you wish to use Git for version control.

## Customization

After obtaining the template (either by downloading the ZIP or cloning and removing the `.git` directory), you can make it your own:

1. **Update Project Details**:
    - Modify the `pom.xml` file to reflect your project's groupId, artifactId, and version.
    - Update the `README.md` to suit your project.

2. **Develop Your Application**:
    - Add your own code, models, controllers, etc., to build your application.

## Dockerfile Usage

This template includes a Dockerfile for easy deployment using Docker. To use it:

1. Ensure the Java version in the Dockerfile matches your project requirements.
2. Replace `com.yourname.CRUD_TEMPLATE.MainApplication` in the ENTRYPOINT command with your project's main class.
3. If your project doesn't use a Maven wrapper (`mvnw`), update the Dockerfile to use your local Maven installation.
4. Optionally, adjust the Maven build command to include or exclude test execution as per your needs.
5. Build and run your Docker image for a containerized version of your application.

This Dockerfile uses a multi-stage build process for efficiency and smaller image size.

## License

This project is licensed under the Creative Commons Zero v1.0 Universal License.
