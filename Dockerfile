# Image used to create the minimal java distribution
FROM debian:9.5-slim AS build

# Install wget to pull java binaries
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    && rm -rf /var/lib/apt/lists/*

ARG JAVA_DOWNLOAD_CHECKSUM=3784cfc4670f0d4c5482604c7c513beb1a92b005f569df9bf100e8bef6610f2e

# Download java and unpack it
RUN cd /opt; \
    wget --no-check-certificate https://download.java.net/java/ga/jdk11/openjdk-11_linux-x64_bin.tar.gz \
    && echo "${JAVA_DOWNLOAD_CHECKSUM}  openjdk-11_linux-x64_bin.tar.gz"  | sha256sum -c \
    && tar zxf openjdk-11_linux-x64_bin.tar.gz \
    && rm -f openjdk-11_linux-x64_bin.tar.gz

# Set java home and run jlink to create a minimal java distribution with modules required for Spring Boot
ENV JAVA_HOME=/opt/jdk-11
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN jlink \
     --module-path /opt/java/jmods \
     --compress=2 \
     --add-modules jdk.jfr,jdk.management.agent,java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument \
     --no-header-files \
     --no-man-pages \
     --output /opt/jdk-mini

# Start a new image and copy just the minimal java distribution from the previous one
FROM debian:9.5-slim
COPY --from=build /opt/jdk-mini /opt/jdk-mini

# Set our java home and other useful envs
ENV JAVA_HOME=/opt/jdk-mini
ENV PATH="$PATH:$JAVA_HOME/bin"
ENV DIRPATH /hcom/servers/pitchfork

# Create some dirs and copy pitchfork jar
RUN mkdir -p $DIRPATH
COPY target/pitchfork.jar $DIRPATH/
RUN chmod 755 $DIRPATH/pitchfork.jar
WORKDIR $DIRPATH

# Set timezone (for logs) and run pitchfork
CMD export TZ=$(date +%Z) &&\
    exec $JAVA_HOME/bin/java \
         $JAVA_JVM_ARGS \
         -jar \
         pitchfork.jar
