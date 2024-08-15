# Step 1: Build the application
FROM gradle:8.5-jdk21 as builder
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Step 2: Run the application
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
