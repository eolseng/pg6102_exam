# Loads a Slim Alpine JDK11 Base Image for smallest possible image size
FROM adoptopenjdk/openjdk11:alpine-slim
WORKDIR app
# Run the application with non-Root Privileges
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
# Copy over .jar-file
COPY ./target/trip.jar .
# Defines the entrypoint for the container
CMD java -jar trip.jar
