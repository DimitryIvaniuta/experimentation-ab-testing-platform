FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY build/libs/ab-testing-platform-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
