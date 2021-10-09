# Builder image
FROM eclipse-temurin:17_35-jdk@sha256:a576b23ab2bdcf3746b10ce9a83004798bf4d9bc9d62d926ffe2c987a7dd3c2b as build

RUN jlink \
     --module-path /opt/java/jmods \
     --compress=2 \
     --add-modules jdk.jfr,jdk.management.agent,java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.management \
     --no-header-files \
     --no-man-pages \
     --output /opt/jdk-mini

# Start a new image and copy just the minimal java distribution from the previous one
FROM debian:11.0-slim
COPY --from=build /opt/jdk-mini /opt/jdk-mini

# Create some dirs and copy pitchfork jar
COPY target/pitchfork.jar /pitchfork/

WORKDIR /pitchfork

# Default jvm options
ENV JAVA_JVM_ARGS="-XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0"

# Set timezone (for logs) and run pitchfork
CMD exec /opt/jdk-mini/bin/java \
         $JAVA_JVM_ARGS \
         -jar \
         pitchfork.jar
