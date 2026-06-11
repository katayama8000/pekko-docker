# ---- Build stage: compile and assemble a fat jar ----
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.19_10_1.12.11_2.13.18 AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached unless build defs change.
COPY project/build.properties project/
COPY project/plugins.sbt      project/
COPY build.sbt                .
RUN sbt update

# Compile and build the assembly jar.
COPY src ./src
RUN sbt assembly

# ---- Runtime stage: small JRE-only image ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/scala-2.13/pekko-docker-assembly-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
