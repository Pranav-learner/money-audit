# Multi-stage Dockerfile for Money Audit Spring Boot Backend

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B || true

COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Tesseract OCR and English trained data
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng curl && \
    rm -rf /var/lib/apt/lists/*

# Create receipts upload directory
RUN mkdir -p /app/uploads/receipts && chmod 777 /app/uploads/receipts

COPY --from=builder /build/target/*.jar app.jar

ENV PORT=8080
ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV RECEIPT_UPLOAD_DIR=/app/uploads/receipts

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/auth/login || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
