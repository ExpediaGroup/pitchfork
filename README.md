[![Build Status](https://travis-ci.org/HotelsDotCom/pitchfork.svg?branch=master)](https://travis-ci.org/HotelsDotCom/pitchfork) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Pitchfork

Pitchfork lifts Zipkin tracing data into Haystack.

## Overview

[Haystack](https://github.com/ExpediaDotCom/haystack) is an [Expedia](https://www.expedia.com/)-backed project to facilitate detection and remediation of problems with enterprise-level web services and websites. Much like [Zipkin](https://github.com/openzipkin/zipkin), its primary goal is to provide an easy to use UI to analyse distributed tracing data, but it offers other features like trend analysis or adaptive alerting.

[Zipkin](https://github.com/openzipkin/zipkin) is the de facto standard for distributed tracing. We understand that migrating to a new system can be difficult and you may want to go back. Pitchfork can help you with this.

### How to build and run Pitchfork

#### Build

To build Pitchfork you need to have Java 11+ and Docker installed. Maven is optional as you can use the Maven Wrapper. 

To compile and run unit tests (Linux or Mac):

    ./mvnw clean verify
    
or (Windows):

    ./mvnw.cmd clean verify

To package:

    ./mvnw clean install
    
To build a Docker image named `hotelsdotcom/pitchfork`:

    docker build -t hotelsdotcom/pitchfork .
    
#### Run

The preferred way to run Pitchfork is via [Docker](https://hub.docker.com/r/hotelsdotcom/pitchfork/).

    docker run -p 9411:9411 hotelsdotcom/pitchfork:latest
    
You can override the default properties by with environment variables (macro case or screaming upper case), for example:

    docker run -p 9411:9411 -e PITCHFORK_FORWARDERS_HAYSTACK_ENABLED=false hotelsdotcom/pitchfork:latest

You can also run it as a normal Java application:

    java -jar pitchfork.jar
    
Or even as a Spring Boot application:

    mvn spring-boot:run
    
##### Health check

The service exposes the following endpoints that can be used to test the app's health and to retrieve useful info:

url       | Description
----------|------------
`/health` | Shows application health information.
`/info`   | Displays application info.

##### Properties

Description | Default
---------------------------------------------------------------|-------------------
server.port                                                    | 9411
pitchfork.validators.accept-null-timestamps                    | true
pitchfork.validators.max-timestamp-drift-seconds               | 3600
pitchfork.forwarders.haystack.kafka.enabled                    | false
pitchfork.forwarders.haystack.kafka.bootstrap-servers          | kafka-service:9092
pitchfork.forwarders.haystack.kafka.topic                      | proto-spans
pitchfork.forwarders.haystack.kinesis.enabled                  | false
pitchfork.forwarders.haystack.kinesis.endpoint-config-type     | REGION
pitchfork.forwarders.haystack.kinesis.region-name              | us-west-1
pitchfork.forwarders.haystack.kinesis.signing-region-name      |
pitchfork.forwarders.haystack.kinesis.service-endpoint         |
pitchfork.forwarders.haystack.kinesis.stream-name              | proto-spans
pitchfork.forwarders.haystack.kinesis.aws-access-key           | 
pitchfork.forwarders.haystack.kinesis.aws-secret-key           | 
pitchfork.forwarders.haystack.kinesis.authentication-type      | DEFAULT
pitchfork.forwarders.logging.enabled                           | false
pitchfork.forwarders.logging.log-full-span                     | false
pitchfork.forwarders.zipkin.http.enabled                       | false
pitchfork.forwarders.zipkin.http.endpoint                      | http://localhost:9411/api/v2/spans
pitchfork.forwarders.zipkin.http.max-inflight-requests         | 256
pitchfork.forwarders.zipkin.http.write-timeout-millis          | 10000
pitchfork.forwarders.zipkin.http.compression-enabled           | true

## Architecture

Pitchfork acts as a collector and forwarder of tracing data.
If you are currently using Zipkin you don't need to do code changes to your service. You can simply change your exporter to use a new endpoint and report the traces to Pitchfork instead of Zipkin.

Pitchfork can be configured to forward incoming spans to: a Zipkin collector; Haystack (using Kafka or Kinesis as a message bus); a logger that just prints the traces as they are received.


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
* [Pitchfork at Docker Hub](https://hub.docker.com/r/hotelsdotcom/pitchfork/)
* [Haystack](https://github.com/ExpediaDotCom/haystack)
* [Zipkin](https://github.com/openzipkin/zipkin)

## License
This project is licensed under the Apache License v2.0 - see the LICENSE.txt file for details.
