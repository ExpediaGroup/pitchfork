[![Build Status](https://travis-ci.org/HotelsDotCom/pitchfork.svg?branch=master)](https://travis-ci.org/HotelsDotCom/pitchfork) [![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://stash.hcom/projects/SHP/repos/haystack-zipkin-proxy/raw/LICENSE)

# Pitchfork

Pitchfork lifts Zipkin tracing data into Haystack.

## Overview

[Haystack](https://github.com/ExpediaDotCom/haystack) is an [Expedia](https://www.expedia.com/)-backed project to facilitate detection and remediation of problems with enterprise-level web services and websites. Much like [Zipkin](https://github.com/openzipkin/zipkin), it's primary goal is to provide an easy to use UI to analyse distributed tracing data, but it offers other features like trend analysis or adaptive alerting.

[Zipkin](https://github.com/openzipkin/zipkin) is the de facto standard for distributed tracing. We understand that migrating to a new system can be difficult and you may want to go back. Pitchfork can help you with this.

### How to build and run Pitchfork

#### Build

To build Pitchfork you need to have Java 11+ and Docker installed. Maven is optional as you can use the Maven Wrapper. 

To compile and run unit tests (Linux or Mac):

    ./mvnw clean verify
    
or (Windows):

    ./mvnw.cmd clean verify

To package as a docker image:

    ./mvnw clean install
    
#### Run

The preferred way to run Pitchfork is via Docker.

    docker run -p 8080:8080 FIXME.docker.com/hotels/pitchfork:latest
    
You can override the default properties by with environment variables (macro case or screaming upper case), for example:

    docker run -p 8080:8080 -e PITCHFORK_FORWARDERS_HAYSTACK_ENABLED=false FIXME.docker.com/hotels/pitchfork:latest

Or you can run it as a normal Java application:

    java -jar pitchfork.jar

##### Properties

Description | Default
------------------------------------------------------|-------------------
server.port                                           | 8080
pitchfork.forwarders.haystack.enabled                 | true
pitchfork.forwarders.haystack.kafka.bootstrap-servers | kafka-service:9092
pitchfork.forwarders.haystack.kafka.topic             | proto-spans
pitchfork.forwarders.logging.enabled                  | false
pitchfork.forwarders.logging.log-full-span            | false
pitchfork.forwarders.zipkin.enabled                   | false
pitchfork.forwarders.zipkin.endpoint                  | http://localhost:9411/api/v2/spans
pitchfork.forwarders.zipkin.max-inflight-requests     | 256
pitchfork.forwarders.zipkin.write-timeout-millis      | 10000
pitchfork.forwarders.zipkin.compression-enabled       | true

## Architecture

Pitchfork acts as a collector and forwarder of tracing data.
If you are currently using Zipkin you don't need to do code changes to your service. You can simply change your exporter to use a new endpoint and report the traces to Pitchfork instead of Zipkin.

Pitchfork can be configured to forward incoming spans to: a Zipkin collector; Haystack (using Kafka as a message bus); a logger that just prints the traces as they are received.


    [ Service A ] ------                               ------> [ Haystack / Kafka ]
                         \                            /
                          ------> [ Pitchfork ] ------
                         /                            \
    [ Service B ] ------                               ------> [ Zipkin ]

These different forwarders can be enabled/disabled separately. Please see the application.yml file for a list of the different configurations available for each. 

## Use Cases

* [Hotels.com](https://www.hotels.com/) - Microservices architecture using stock Zipkin data and libraries ([Spring Cloud Sleuth](https://cloud.spring.io/spring-cloud-sleuth/), [Brave](https://github.com/openzipkin/brave), etc.) that report to Pitchfork which handles the integration with [Haystack](https://github.com/ExpediaDotCom/haystack). 

## Contributing

None of these are essential for a pull request to be approved but they all help.

* We don't formalize coding standards for this project other than using spaces and not tabs (we're not animals). Please try to use the existing code as a convention guide.
* Make sure you test your code before opening a new pr. You will need Java 11+ and Docker to run the integration tests. Note that some classes in this project do not have unit tests: this is because either they are not trivial to test or having test coverage at this level would not add much.
* Try to have a clean git history. Use git rebase when pulling changes from master and, if you have multiple (related) commits do try and squash them into a single one.

## References
* [Haystack](https://github.com/ExpediaDotCom/haystack)
* [Zipkin](https://github.com/openzipkin/zipkin)

## License
This project is licensed under the Apache License v2.0 - see the LICENSE.txt file for details.
