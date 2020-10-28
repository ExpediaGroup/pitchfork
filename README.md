<h1 align="left">
  <img width="500" alt="Pitchfork" src="images/pitchfork_logo.svg">
</h1>

[![Build Status](https://github.com/ExpediaGroup/pitchfork/workflows/Build/badge.svg)](https://github.com/ExpediaGroup/pitchfork/actions?query=workflow:"Build")
[![Release](https://img.shields.io/github/release/expediagroup/pitchfork.svg)](https://img.shields.io/github/release/expediagroup/pitchfork.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![GitHub site](https://img.shields.io/badge/GitHub-site-blue.svg)](https://expediagroup.github.io/pitchfork/)
[![Docker Pulls](https://img.shields.io/docker/pulls/expediagroup/pitchfork.svg?cacheSeconds=86400)](https://hub.docker.com/r/expediagroup/pitchfork/)

# Pitchfork

Pitchfork lifts Zipkin tracing data into Haystack.

You can find more detailed documentation at [expediagroup.github.io/pitchfork](https://expediagroup.github.io/pitchfork/).

## Overview

[Haystack](https://github.com/ExpediaGroup/haystack) is an [Expedia](https://www.expedia.com/) backed project to facilitate detection and remediation of problems with enterprise-level web services and websites. Much like [Zipkin](https://github.com/openzipkin/zipkin), its primary goal is to provide an easy to use UI to analyse distributed tracing data, but it offers other features like trend analysis or adaptive alerting.

[Zipkin](https://github.com/openzipkin/zipkin) is the de facto standard for distributed tracing. We understand that migrating to a new system can be difficult and you may want to go back. Pitchfork can help you with this.

### How to build and run Pitchfork

#### Build

To build Pitchfork you need to have Java 15+ and Docker installed. Maven is optional as you can use the Maven Wrapper.

To compile and run unit tests (Linux or Mac):

    ./mvnw clean verify

or (Windows):

    ./mvnw.cmd clean verify

To package:

    ./mvnw clean install

To build a Docker image named `expediagroup/pitchfork`:

    docker build -t expediagroup/pitchfork .

Alternatively we also provide a makefile that you can use to run the tests or build a docker image for Pitchfork:

    make test

or

    make build


#### Running Pitchfork

The preferred way to run Pitchfork is via [Docker](https://hub.docker.com/r/expediagroup/pitchfork/).

    docker run -p 9411:9411 expediagroup/pitchfork:latest

You can override the default properties with environment variables (macro case or screaming upper case), for example:

    docker run -p 9411:9411 -e PITCHFORK_FORWARDERS_LOGGING_ENABLED=true expediagroup/pitchfork:latest

You can also run it as a normal Java application:

    java -jar pitchfork.jar

Or as a Spring Boot application:

    mvn spring-boot:run

You can find more info on how to configure Pitchfork in our [documentation](https://expediagroup.github.io/pitchfork/) page, including how to install it using alternative methods like Helm.

## Architecture

Pitchfork acts as a collector and forwarder of tracing data.
If you are currently using Zipkin you don't need to do code changes to your service. You can simply change your exporter to use a new endpoint and report the traces to Pitchfork instead of Zipkin.
Pitchfork accepts Zipkin spans in json v1 and v2, thrift and protobuf. You can also configure this application to consume Zipkin spans from a Kafka broker.

Pitchfork can be configured to forward incoming spans to: a Zipkin collector; Haystack (using Kafka or Kinesis as a message bus); a logger that just prints the traces as they are received.


    [ Service A ] ------                               ------> [ Haystack / Kafka ]
                         \                            /
                          ------> [ Pitchfork ] ------
                         /                            \
    [ Service B ] ------                               ------> [ Zipkin ]

These different forwarders can be enabled/disabled separately. Please see the application.yml file for a list of the different configurations available for each, or refer to the table above with the list of properties you can configure.

## Use Cases

* [Hotels.com](https://www.hotels.com/) - Microservices architecture using stock Zipkin data and libraries ([Spring Cloud Sleuth](https://cloud.spring.io/spring-cloud-sleuth/), [Brave](https://github.com/openzipkin/brave), etc.) that report to Pitchfork which handles the integration with [Haystack](https://github.com/ExpediaDotCom/haystack).

## Contributing

Please refer to our [CONTRIBUTING](./CONTRIBUTING.md) file.

## References
* [Pitchfork Documentation](https://expediagroup.github.io/pitchfork/)
* [Pitchfork at Docker Hub](https://hub.docker.com/r/expediagroup/pitchfork/)
* [Haystack](https://github.com/ExpediaDotCom/haystack/)
* [Zipkin](https://github.com/openzipkin/zipkin/)

## License
This project is licensed under the Apache License v2.0 - see the
[LICENSE](LICENSE) file for details.
