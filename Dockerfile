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

# Set java home and run jlink to create a minimal java distribution with modules required for Spring Boot
ENV JAVA_HOME=/opt/jdk-14+36
ENV PATH="$PATH:$JAVA_HOME/bin"

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

# Set our java home and other useful envs
ENV JAVA_HOME=/opt/jdk-mini
ARG PATH="$PATH:$JAVA_HOME/bin"
ARG DIRPATH=/pitchfork

# Create some dirs and copy pitchfork jar
RUN mkdir -p $DIRPATH
COPY target/pitchfork.jar $DIRPATH/

WORKDIR $DIRPATH

# Set timezone (for logs) and run pitchfork
CMD export TZ=$(date +%Z) &&\
    exec $JAVA_HOME/bin/java \
         $JAVA_JVM_ARGS \
         -jar \
         pitchfork.jar
