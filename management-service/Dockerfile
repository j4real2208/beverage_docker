FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/management-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8090

CMD ["java", "-jar", "app.jar"]
