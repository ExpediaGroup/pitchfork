# Image used to create the minimal java distribution
FROM debian:10.3-slim AS build

# Install wget to pull java binaries
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    && rm -rf /var/lib/apt/lists/*

ARG JAVA_DOWNLOAD_CHECKSUM=6c06853332585ab58834d9e8a02774b388e6e062ef6c4084b4f058c67f2e81b5

# Download java and unpack it
RUN cd /opt; \
    wget --no-check-certificate https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14%2B36/OpenJDK14U-jdk_x64_linux_hotspot_14_36.tar.gz \
    && echo "${JAVA_DOWNLOAD_CHECKSUM}  OpenJDK14U-jdk_x64_linux_hotspot_14_36.tar.gz"  | sha256sum -c \
    && tar zxf OpenJDK14U-jdk_x64_linux_hotspot_14_36.tar.gz \
    && rm -f OpenJDK14U-jdk_x64_linux_hotspot_14_36.tar.gz

ENV PATH="$PATH:/opt/jdk-14+36/bin"

RUN jlink \
     --module-path /opt/java/jmods \
     --compress=2 \
     --add-modules jdk.jfr,jdk.management.agent,java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,jdk.management \
     --no-header-files \
     --no-man-pages \
     --output /opt/jdk-mini

# Start a new image and copy just the minimal java distribution from the previous one
FROM debian:10.3-slim
COPY --from=build /opt/jdk-mini /opt/jdk-mini

# Create some dirs and copy pitchfork jar
COPY target/pitchfork.jar /pitchfork/

WORKDIR /pitchfork

# Default jvm options
ENV JAVA_JVM_ARGS="-XX:MaxRAMPercentage=80.0"

# Set timezone (for logs) and run pitchfork
CMD exec /opt/jdk-mini/bin/java \
         $JAVA_JVM_ARGS \
         -jar \
         pitchfork.jar
