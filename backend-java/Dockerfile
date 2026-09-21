FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . /app
RUN chmod +x ./mvnw || true
RUN ./apache-maven-3.9.6/bin/mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Install Python for the email script
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*
# Create a symlink so "python" maps to "python3"
RUN ln -s /usr/bin/python3 /usr/bin/python || true

COPY --from=build /app/target/*.jar app.jar
COPY send_email.py .

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]

