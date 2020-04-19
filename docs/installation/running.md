---
id: running
title: How to Run
---

## Run the Docker image

The preferred way to run Pitchfork is with [Docker](https://hub.docker.com/r/expediagroup/pitchfork/).

    docker run -p 9411:9411 expediagroup/pitchfork:latest

You can override the default properties with environment variables (macro case or screaming upper case), for example:

    docker run -p 9411:9411 -e PITCHFORK_FORWARDERS_LOGGING_ENABLED=true expediagroup/pitchfork:latest

## Run the jar

You can also run it as a normal Java application:

    java -jar pitchfork.jar

## Run as a Spring Boot application

Or as a Spring Boot application:

    mvn spring-boot:run

You can find more info on how to configure Pitchfork in our [documentation](https://expediagroup.github.io/pitchfork/) page.
