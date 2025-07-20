FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/db-handler-1.0-SNAPSHOT.jar app.jar

# Install coreutils for cat command
RUN apt-get update && apt-get install -y coreutils jq yq vim && rm -rf /var/lib/apt/lists/*

EXPOSE 9999

CMD ["java", "-jar", "app.jar"]
