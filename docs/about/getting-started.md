---
id: getting-started
title: Getting Started
---

## Configuring different ingresses

### HTTP

HTTP is the default ingress and is always enabled. The following properties can be overridden.

| Property name | Default value | Description                            |
|:--------------|:--------------|:---------------------------------------|
| SERVER_PORT   | 9411          | HTTP port where Pitchfork is listening |

### Kafka

Kafka ingress is disabled by default. You can enable and configure it using the following properties.

| Property name                                   | Default value      | Description                                                                                                                                                      |
|:------------------------------------------------|:-------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PITCHFORK_INGRESS_KAFKA_ENABLED                 | false              | If enabled Pitchfork will read Zipkin spans from the configured Kafka topic                                                                                      |
| PITCHFORK_INGRESS_KAFKA_BOOTSTRAP_SERVERS       | kafka-service:9092 | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster                                                                    |
| PITCHFORK_INGRESS_KAFKA_NUMBER_CONSUMERS        | 4                  | The number of consumer threads polling Kafka                                                                                                                     |
| PITCHFORK_INGRESS_KAFKA_ENABLE_AUTO_COMMIT      | true               | If true the consumer's offset will be periodically committed in the background                                                                                   |
| PITCHFORK_INGRESS_KAFKA_AUTO_COMMIT_INTERNAL_MS | 1000               | The frequency in milliseconds that the consumer offsets are auto-committed to Kafka                                                                              |
| PITCHFORK_INGRESS_KAFKA_POLL_DURATION_MS        | 1000               | The maximum time to block waiting for new records                                                                                                                |
| PITCHFORK_INGRESS_KAFKA_AUTO_OFFSET_RESET       | earliest           | What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server. Possible values are earliest, latest or none |
| PITCHFORK_INGRESS_KAFKA_SESSION_TIMEOUT_MS      | 60000              | The timeout used to detect consumer failures when using Kafka's group management facility                                                                        |
| PITCHFORK_INGRESS_KAFKA_SOURCE_TOPICS           | zipkin             | List of Kafka topics to subscribe to                                                                                                                             |
| PITCHFORK_INGRESS_KAFKA_SOURCE_FORMAT           | JSON_V2            | Format/encoding of the spans in the Kafka topic. Possible values are JSON_V1, THRIFT, JSON_V2 or PROTO3                                                          |
{: .tablelines}

### RabbitMQ

RabbitMQ ingress is disabled by default. You can enable and configure it using the following properties.

| Property name                            | Default value | Description                                                                                                |
|:-----------------------------------------|:--------------|:-----------------------------------------------------------------------------------------------------------|
| PITCHFORK_INGRESS_RABBITMQ_ENABLED       | false         | If enabled Pitchfork will read Zipkin spans from the configured RabbitMQ queue                             |
| PITCHFORK_INGRESS_RABBITMQ_USER          | guest         | The user name to use when connecting to the broker                                                         |
| PITCHFORK_INGRESS_RABBITMQ_PASSWORD      | guest         | The password to use when connecting to the broker                                                          |
| PITCHFORK_INGRESS_RABBITMQ_VIRTUAL_HOST  | /             | The virtual host to use for connections                                                                    |
| PITCHFORK_INGRESS_RABBITMQ_HOST          | localhost     | The host for the broker                                                                                    |
| PITCHFORK_INGRESS_RABBITMQ_PORT          | 5762          | The port for the broker                                                                                    |
| PITCHFORK_INGRESS_RABBITMQ_QUEUE_NAME    | zipkin        | The name of the queue to read spans from                                                                   |
| PITCHFORK_INGRESS_RABBITMQ_SOURCE_FORMAT | JSON_V2       | Format/encoding of the spans in the RabbitMQ queue. Possible values are JSON_V1, THRIFT, JSON_V2 or PROTO3 |

## Configuring different outputs

### Kafka

Kafka output is disabled by default. You can enable and configure it using the following properties.

| Property name                                         | Default value      | Description                                                                                   |
|:------------------------------------------------------|:-------------------|:----------------------------------------------------------------------------------------------|
| PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_ENABLED           | false              | If enabled Pitchfork will forward spans to a Kafka broker                                     |
| PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_BOOTSTRAP_SERVERS | kafka-service:9092 | A list of host/port pairs to use for establishing the initial connection to the Kafka cluster |
| PITCHFORK_FORWARDERS_HAYSTACK_KAFKA_TOPIC             | proto-spans        | The name of the Kafka topic where the spans will be submitted to                              |

### Kinesis

Kinesis output is disabled by default. You can enable and configure it using the following properties.

| Property name                                                             | Default value                           | Description                                                                                                                 |
|:--------------------------------------------------------------------------|:----------------------------------------|:----------------------------------------------------------------------------------------------------------------------------|
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_ENABLED                             | false                                   | If enabled Pitchfork will forward spans to a Kinesis stream                                                                 |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_STREAM_NAME                         | proto-spans                             | The name of the Kinesis stream where the spans will be submitted to                                                         |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_CLIENT_CONFIG_TYPE                  | REGION                                  | What type of endpoint configuration to use. Possible values are REGION or ENDPOINT                                          |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_CLIENT_REGION_REGION_NAME           | us-west-1                               | Used when the kinesis client config-type is REGION. Region is used to determine the service endpoint and the signing region |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_CLIENT_ENDPOINT_SERVICE_ENDPOINT    | https://kinesis.us-west-1.amazonaws.com | Used when kinesis client config-type is ENDPOINT. The service endpoint (with or without protocol)                           |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_CLIENT_ENDPOINT_SIGNING_REGION_NAME | us-west-1                               | Used when kinesis client config-type is ENDPOINT. The region to use for signing of requests                                 |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_AUTH_CONFIG_TYPE                    | DEFAULT                                 | Authentication method to use. Possible values are DEFAULT or BASIC                                                          |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_AUTH_BASIC_AWS_ACCESS_KEY           | accesskey                               | Used when authentication-type is BASIC. The AWS access key                                                                  |
| PITCHFORK_FORWARDERS_HAYSTACK_KINESIS_AUTH_basic_AWS_SECRET_KEY           | secretkey                               | Used when authentication-type is BASIC. The AWS secret access key                                                           |

### Zipkin HTTP

Zipkin output is disabled by default. You can enable and configure it using the following properties.

| Property name                                          | Default value                      | Description                                                                     |
|:-------------------------------------------------------|:-----------------------------------|:--------------------------------------------------------------------------------|
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_ENABLED               | false                              | If enabled Pitchfork will forward spans to an HTTP Zipkin server                |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_ENDPOINT              | http://localhost:9411/api/v2/spans | The POST url for the Zipkin http reporter                                       |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_MAX_INFLIGHT_REQUESTS | 256                                | Number of max inflight requests                                                 |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_WRITE_TIMEOUT_MILLIS  | 10000                              | Write timeout in milliseconds                                                   |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_COMPRESSION_ENABLED   | true                               | Set to true for spans to be gzipped before transport                            |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_MAX_IDLE_CONNECTIONS  | 50                                 | Max idle connections for the Zipkin reporter connection pool                    |
| PITCHFORK_FORWARDERS_ZIPKIN_HTTP_IGNORE_SSL_ERRORS     | false                              | When true, ignores all SSL errors when connecting to the upstream Zipkin server |

## Other options

### Logging

It may be useful to enable the logging of spans for troubleshooting.

To enable the logging of just the trace id:

```yaml
PITCHFORK_FORWARDERS_LOGGING_ENABLED=true
```

To enable the logging of the entire span received:

```yaml
PITCHFORK_FORWARDERS_LOGGING_ENABLED=true
PITCHFORK_FORWARDERS_LOGGING_LOG_FULL_SPAN=true
```

### Metrics

By default Pitchfork exposes both a /metrics and a /prometheus endpoint (see section [Endpoints](#Endpoints))
You can also report data to a Graphite collector by configuring the host and port:

```
MANAGEMENT_METRICS_EXPORT_GRAPHITE_ENABLED=true
MANAGEMENT_METRICS_EXPORT_GRAPHITE_HOST=graphite.example.com
MANAGEMENT_METRICS_EXPORT_GRAPHITE_PORT=9004
```

You can also use the following properties to add tag to the metrics and (for unidimensional systems like graphite) convert them into prefixes.
This is useful if you have multiple instances of Pitchfork running and you would want your metrics to look like this: `pitchfork.instance-01.metric-name=123`

```
MANAGEMENT_METRICS_MANAGEMENT_METRICS_TAGS_APP=pitchfork
MANAGEMENT_METRICS_MANAGEMENT_METRICS_TAGS_INSTANCE=instance-01
MANAGEMENT_METRICS_MANAGEMENT_METRICS_EXPORT_GRAPHITE_TAGS_AS_PREFIX=APP,INSTANCE
```

### Validators

You can also configure Pitchfork to discard spans with a null timestamp:

```yaml
PITCHFORK_VALIDATORS_ACCEPT_NULL_TIMESTAMPS=false
```

Or to discard spans if the difference between the timestamp of the span and the timestamp of the server is over x seconds.

Example for 60 seconds (-1 to disable):

```yaml
MAX_TIMESTAMP_DRIFT_SECONDS=60
```

### Endpoints

The service exposes the following endpoints that can be used to test the app's health and to retrieve useful info

| Endpoint    | Description                                                                       |
|:------------|:----------------------------------------------------------------------------------|
| /health     | Shows application health information                                              |
| /info       | Displays application info                                                         |
| /metrics    | Metrics endpoint that can be used to examine metrics collected by the application |
| /prometheus | Endpoint that presents metrics in a format that can be scraped by Prometheus      |
