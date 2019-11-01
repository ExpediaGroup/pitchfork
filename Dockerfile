# Image used to create the minimal java distribution
FROM debian:10.1-slim AS build

# Install wget to pull java binaries
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    && rm -rf /var/lib/apt/lists/*

ARG JAVA_DOWNLOAD_CHECKSUM=2226366d7dffb3eb4ec3d14101f4ddb4259195aa43bb319a93810447b8384930

# Download java and unpack it
RUN cd /opt; \
    wget --no-check-certificate https://github.com/AdoptOpenJDK/openjdk13-binaries/releases/download/jdk-13.0.1%2B9/OpenJDK13U-jdk_x64_linux_hotspot_13.0.1_9.tar.gz \
    && echo "${JAVA_DOWNLOAD_CHECKSUM}  OpenJDK13U-jdk_x64_linux_hotspot_13.0.1_9.tar.gz"  | sha256sum -c \
    && tar zxf OpenJDK13U-jdk_x64_linux_hotspot_13.0.1_9.tar.gz \
    && rm -f OpenJDK13U-jdk_x64_linux_hotspot_13.0.1_9.tar.gz

# Set java home and run jlink to create a minimal java distribution with modules required for Spring Boot
ENV JAVA_HOME=/opt/jdk-13.0.1+9
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN jlink \
     --module-path /opt/java/jmods \
     --compress=2 \
     --add-modules jdk.jfr,jdk.management.agent,java.base,java.logging,java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument \
     --no-header-files \
     --no-man-pages \
     --output /opt/jdk-mini

# Start a new image and copy just the minimal java distribution from the previous one
FROM debian:10.1-slim
COPY --from=build /opt/jdk-mini /opt/jdk-mini

# Set our java home and other useful envs
ENV JAVA_HOME=/opt/jdk-mini
ENV PATH="$PATH:$JAVA_HOME/bin"
ENV DIRPATH /pitchfork


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
