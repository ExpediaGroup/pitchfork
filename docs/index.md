<style>
.tablelines table, .tablelines td, .tablelines th {
        border: 1px solid black;
        }
</style>
# Pitchfork

[Haystack](https://expediadotcom.github.io/haystack/) is an Expedia-backed project to facilitate detection and remediation of problems with enterprise-level web services and websites. Much like Zipkin, its primary goal is to provide an easy to use UI to analyse distributed tracing data, but it offers other features like trend analysis or adaptive alerting.

[Zipkin](http://zipkin.io) is the de facto standard for distributed tracing. We understand that migrating to a new system can be difficult and you may want to go back. Pitchfork can help you with this.

## Running

### From a jar

```bash
java -jar pitchfork.jar
```

### With Docker

_Note: When using Docker it is preferable to use the posix notation for the properties (eg. FOO_BAR instead of foo.bar)._

You can run Pitchfork as a Docker container. You can find all available tags in [Docker Hub](https://hub.docker.com/r/hotelsdotcom/pitchfork/).

```bash
docker run -p 9411:9411 hotelsdotcom/pitchfork:latest
```

You can override the default properties with environment variables, for example:

```bash
docker run -p 9411:9411 -e PITCHFORK_FORWARDERS_LOGGING_ENABLED=true hotelsdotcom/pitchfork:latest
```

## Configuring different ingresses

### HTTP

HTTP is the default ingress and is always enabled.
The following properties can be overridden.

| Property name | Default value | Description                            | 
| ------------- | ------------- | -------------------------------------- |
| server.port   | 9411          | HTTP port where Pitchfork is listening | 
{: .tablelines}

### Kafka

Kafka ingress is disabled by default. You can enable and configure it using the following properties.

| Property name                                   | Default value      | Description                                                                                                                                                      |
| ----------------------------------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| pitchfork.ingress.kafka.enabled                 | false              | If enabled Pitchfork will read Zipkin spans from the configured Kafka topic                                                                                      |
| pitchfork.ingress.kafka.bootstrap-servers       | kafka-service:9092 | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster                                                                    |
| pitchfork.ingress.kafka.enable-auto-commit      | true               | If true the consumer's offset will be periodically committed in the background                                                                                   |
| pitchfork.ingress.kafka.auto-commit-internal-ms | 1000               | The frequency in milliseconds that the consumer offsets are auto-committed to Kafka                                                                              |
| pitchfork.ingress.kafka.poll-duration-ms        | 1000               | The maximum time to block waiting for new records                                                                                                                |
| pitchfork.ingress.kafka.auto-offset-reset       | earliest           | What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server. Possible values are earliest, latest or none |
| pitchfork.ingress.kafka.session-timeout-ms      | 60000              | The timeout used to detect consumer failures when using Kafka's group management facility                                                                        |
| pitchfork.ingress.kafka.source-topics           | zipkin             | List of Kafka topics to subscribe to                                                                                                                             |
| pitchfork.ingress.kafka.source-format           | JSON_V2            | Format/encoding of the spans in the Kafka topic. Possible values are JSON_V1, THRIFT, JSON_V2 or PROTO3                                                          |
{: .tablelines}

### RabbitMQ

RabbitMQ ingress is disabled by default. You can enable and configure it using the following properties.

| Property name                            | Default value | Description                                                                                                |
| ---------------------------------------- | ------------- | ---------------------------------------------------------------------------------------------------------- |
| pitchfork.ingress.rabbitmq.enabled       | false         | If enabled Pitchfork will read Zipkin spans from the configured RabbitMQ queue                             |         
| pitchfork.ingress.rabbitmq.user          | guest         | The user name to use when connecting to the broker                                                         |
| pitchfork.ingress.rabbitmq.password      | guest         | The password to use when connecting to the broker                                                          |
| pitchfork.ingress.rabbitmq.virtual-host  | /             | The virtual host to use for connections                                                                    |
| pitchfork.ingress.rabbitmq.host          | localhost     | The host for the broker                                                                                    |
| pitchfork.ingress.rabbitmq.port          | 5762          | The port for the broker                                                                                    |
| pitchfork.ingress.rabbitmq.queue-name    | zipkin        | The name of the queue to read spans from                                                                   |
| pitchfork.ingress.rabbitmq.source-format | JSON_V2       | Format/encoding of the spans in the RabbitMQ queue. Possible values are JSON_V1, THRIFT, JSON_V2 or PROTO3 | 
{: .tablelines}

## Configuring different outputs

### Kafka

Kafka output is disabled by default. You can enable and configure it using the following properties.

| Property name                                         | Default value      | Description                                                                                   |
| ----------------------------------------------------- | ------------------ | --------------------------------------------------------------------------------------------- |
| pitchfork.forwarders.haystack.kafka.enabled           | false              | If enabled Pitchfork will forward spans to a Kafka broker                                     |
| pitchfork.forwarders.haystack.kafka.bootstrap-servers | kafka-service:9092 | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster |
| pitchfork.forwarders.haystack.kafka.topic             | proto-spans        | The name of the Kafka topic where the spans will be submitted to                              |         

### Kinesis

Kinesis output is disabled by default. You can enable and configure it using the following properties.

| Property name                                              | Default value | Description                                                                                                       |
| ---------------------------------------------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------- |
| pitchfork.forwarders.haystack.kinesis.enabled              | false         | If enabled Pitchfork will forward spans to a Kinesis stream                                                       |
| pitchfork.forwarders.haystack.kinesis.stream-name          | proto-spans   | The name of the Kinesis stream where the spans will be submitted to                                               |
| pitchfork.forwarders.haystack.kinesis.endpoint-config-type | REGION        | What type of endpoint configuration to use. Possible values are REGION or CONFIGURATION                           |
| pitchfork.forwarders.haystack.kinesis.authentication-type  | DEFAULT       | Authentication method to use. Possible values are DEFAULT or BASIC                                                |
| pitchfork.forwarders.haystack.kinesis.region-name          | us-west-1     | Used when endpoint-config-type is REGION. Region is used to determine the service endpoint and the signing region |         
| pitchfork.forwarders.haystack.kinesis.service-endpoint     |               | Used when endpoint-config-type is CONFIGURATION. The service endpoint (with or without protocol)                  |         
| pitchfork.forwarders.haystack.kinesis.signing-region-name  | us-west-1     | Used when endpoint-config-type is CONFIGURATION. The region to use for signing of requests                        |
| pitchfork.forwarders.haystack.kinesis.aws-access-key       | accesskey     | Used when authentication-type is BASIC. The AWS access key                                                        |
| pitchfork.forwarders.haystack.kinesis.aws-secret-key       | secretkey     | Used when authentication-type is BASIC. The AWS secret access key                                                 |

### Zipkin HTTP

Zipkin output is disabled by default. You can enable and configure it using the following properties.

| Property name                                          | Default value                      | Description                                                      |
| ------------------------------------------------------ | ---------------------------------- | ---------------------------------------------------------------- |
| pitchfork.forwarders.zipkin.http.enabled               | false                              | If enabled Pitchfork will forward spans to an HTTP Zipkin server |
| pitchfork.forwarders.zipkin.http.endpoint              | http://localhost:9411/api/v2/spans | The POST url for the Zipkin http reporter                        |
| pitchfork.forwarders.zipkin.http.max-inflight-requests | 256                                | Number of max inflight requests                                  |
| pitchfork.forwarders.zipkin.http.write-timeout-millis  | 10000                              | Write timeout in milliseconds                                    |
| pitchfork.forwarders.zipkin.http.compression-enabled   | true                               | Set to true for spans to be gzipped before transport             |
| pitchfork.forwarders.zipkin.http.max-idle-connections  | 50                                 | Max idle connections for the Zipkin reporter connection pool     |

## Other options

### Logging

It may be useful to enable the logging of spans for troubleshooting.

To enable the logging of just the trace id:

```yaml
pitchfork.forwarders.logging.enabled=true
```

To enable the logging of the entire span received:

```yaml
pitchfork.forwarders.logging.enabled=true
pitchfork.forwarders.logging.log-full-span=true
```

### Validators

You can also configure Pitchfork to discard spans with a null timestamp:

```yaml
pitchfork.validators.accept-null-timestamps=false
```

Or to discard spans if the difference between the timestamp of the span and the timestamp of the server is over x seconds. 

Example for 60 seconds (-1 to disable):

```yaml
max-timestamp-drift-seconds=60
```

### Endpoints

The service exposes the following endpoints that can be used to test the app's health and to retrieve useful info

| Endpoint    | Description                                                                       |
| ----------- | --------------------------------------------------------------------------------- |
| /health     | Shows application health information                                              |
| /info       | Displays application info                                                         |
| /metrics    | Metrics endpoint that can be used to examine metrics collected by the application |
| /prometheus | Endpoint that presents metrics in a format that can be scraped by Prometheus      |
