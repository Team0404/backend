# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :order-service:bootJar -x test --no-daemon

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/order-service/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 19094
ENTRYPOINT ["java", "-jar", "app.jar"]
