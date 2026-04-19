FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/nestchat-server-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
