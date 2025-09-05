# Stage 1: Build stage
FROM maven:3.9.11-amazoncorretto-21 AS build

# Copy Maven files for dependency resolution
COPY pom.xml ./
COPY .mvn .mvn

# Copy application source code
COPY src src

# Build the project and create the executable JAR
RUN mvn clean install -DskipTests

# Stage 2: Run stage
FROM amazoncorretto:21

# Set working directory
WORKDIR githubscreenshotmailer

# Copy the JAR file from the build stage
COPY --from=build target/*.jar githubscreenshotmailer.jar

# Expose port 1926
EXPOSE 1926

# Set the entrypoint command for running the application
ENTRYPOINT ["java", "--enable-preview", "-jar", "githubscreenshotmailer.jar"]