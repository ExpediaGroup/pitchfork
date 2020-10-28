---
id: building
title: How to Build
---

To build Pitchfork you need to have Java 15+ and Docker installed. Maven is optional as you can use the Maven Wrapper.

### Using the Maven Wrapper

To compile and run unit tests (Linux or Mac):

    ./mvnw clean verify

or (Windows):

    ./mvnw.cmd clean verify

To package:

    ./mvnw clean install

### Using the Makefile

Alternatively we also provide a makefile that you can use to run the tests or build a docker image for Pitchfork:

    make test

or

    make build

### Using Docker

To build a Docker image named `expediagroup/pitchfork`:

    docker build -t expediagroup/pitchfork .
