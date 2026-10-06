FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y maven --no-install-recommends && rm -rf /var/lib/apt/lists/*
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/maison-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
