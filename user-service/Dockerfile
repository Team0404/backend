# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :user-service:bootJar -x test --no-daemon

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/user-service/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 19091
ENTRYPOINT ["java", "-jar", "app.jar"]
