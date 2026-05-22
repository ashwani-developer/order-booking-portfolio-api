# Multi-stage build for smaller final image
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
COPY src/ src/

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S trading && adduser -S trading -G trading
USER trading

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
