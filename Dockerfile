FROM gradle:8.10.2-jdk21-alpine AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./

COPY src/ src/

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"

RUN gradle clean build -x test

FROM bellsoft/liberica-runtime-container:jdk-21-glibc

WORKDIR /app

RUN addgroup --system --gid 1000 javauser && \
    adduser --system --uid 1000 --ingroup javauser javauser

COPY --from=builder /app/src/main/resources/keystore.p12 /app/keystore.p12
COPY --from=builder /app/src/main/resources/cert.pem /app/cert.pem
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R javauser:javauser /app

USER javauser

ENV SSL_KEYSTORE_LOCATION=file:/app/keystore.p12
ENV SSL_CERT_LOCATION=file:/app/cert.pem

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]