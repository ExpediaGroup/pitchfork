# Builder image
FROM adoptopenjdk/openjdk16:jdk-16.0.1_9-debian as build

RUN jlink \
     --module-path /opt/java/jmods \
     --compress=2 \
     --add-modules jdk.jfr,jdk.management.agent,java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.management \
     --no-header-files \
     --no-man-pages \
     --output /opt/jdk-mini

# Start a new image and copy just the minimal java distribution from the previous one
FROM debian:stable-slim
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
