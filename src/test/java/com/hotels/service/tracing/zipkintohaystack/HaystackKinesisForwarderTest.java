package com.hotels.service.tracing.zipkintohaystack;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.*;
import com.expedia.open.tracing.Span;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.util.Optional;

import static com.amazonaws.services.kinesis.model.ShardIteratorType.TRIM_HORIZON;
import static com.hotels.service.tracing.zipkintohaystack.TestUtils.AWAIT;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {HaystackKinesisForwarderTest.Initializer.class})
class HaystackKinesisForwarderTest {

    @Container
    private static final LocalStackContainer kinesisContainer = new LocalStackContainer().withServices(KINESIS);

    private static String KINESIS_SERVICE_ENDPOINT;
    private static AmazonKinesis kinesisClient;

    @LocalServerPort
    private int localServerPort;

    @BeforeAll
    static void setup() {
        setKinesisServiceEndpoint();

        // create output topic/stream
        kinesisClient = setupKinesisClient();
        kinesisClient.createStream("proto-spans", 1);
    }

    private static void setKinesisServiceEndpoint() {
        // https://github.com/localstack/localstack/blob/e479afa41df908305c4177276237925accc77e10/localstack/ext/java/src/test/java/cloud/localstack/BasicFunctionalityTest.java#L54
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");

        var serviceEndpoint = kinesisContainer.getEndpointConfiguration(KINESIS).getServiceEndpoint();
        var endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, "us-west-1");

        KINESIS_SERVICE_ENDPOINT = endpointConfiguration.getServiceEndpoint();
    }

    private static AmazonKinesis setupKinesisClient() {
        var endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(KINESIS_SERVICE_ENDPOINT, "us-west-1");

        return AmazonKinesisClientBuilder.standard()
                .withCredentials(kinesisContainer.getDefaultCredentialsProvider())
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    private static Optional<Span> deserialize(byte[] data) {
        try {
            return ofNullable(Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
        }
    }

    @Test
    void shouldForwardTracesToKinesis() throws Exception {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "abc";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter();
        reporter.report(zipkinSpan);

        AWAIT.untilAsserted(() -> assertThat(streamStatus("proto-spans")).isEqualTo("ACTIVE"));

        DescribeStreamResult streamResult = kinesisClient.describeStream("proto-spans");
        GetShardIteratorRequest shardIteratorRequest = new GetShardIteratorRequest()
                .withShardIteratorType(TRIM_HORIZON)
                .withShardId(streamResult.getStreamDescription().getShards().iterator().next().getShardId()) // there's just 1 shard
                .withStreamName("proto-spans");

        GetShardIteratorResult shardIterator = kinesisClient.getShardIterator(shardIteratorRequest);
        GetRecordsRequest getRecordsRequest = new GetRecordsRequest().withShardIterator(shardIterator.getShardIterator());

        // proxy is async, and kafka is async too, so we retry our assertions until they are true
        AWAIT.untilAsserted(() -> {
            GetRecordsResult records = kinesisClient.getRecords(getRecordsRequest);

            assertThat(records.getRecords()).isNotEmpty();

            Record record = records.getRecords().iterator().next(); // there's only one element so get first

            Optional<Span> span = deserialize(record.getData().array());

            assertThat(span).isPresent();
            assertThat(span.get().getTraceId()).isEqualTo(traceId);
            assertThat(span.get().getSpanId()).isEqualTo(spanId);
            assertThat(span.get().getParentSpanId()).isEqualTo(parentId);
            assertThat(span.get().getStartTime()).isEqualTo(timestamp);
            assertThat(span.get().getDuration()).isEqualTo(duration);
        });
    }

    private String streamStatus(String streamName) {
        DescribeStreamResult streamResult = kinesisClient.describeStream(streamName);
        return streamResult.getStreamDescription().getStreamStatus();
    }

    /**
     * Create reporter.
     */
    private AsyncReporter<zipkin2.Span> setupReporter() {
        var sender = OkHttpSender.newBuilder()
                .encoding(Encoding.PROTO3)
                .endpoint("http://localhost:" + localServerPort + "/api/v2/spans")
                .build();
        return AsyncReporter.create(sender);
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            var values = TestPropertyValues.of(
                    "pitchfork.forwarders.haystack.kinesis.enabled=true",
                    "pitchfork.forwarders.haystack.kinesis.auth.config-type=BASIC",
                    "pitchfork.forwarders.haystack.kinesis.client.config-type=ENDPOINT",
                    "pitchfork.forwarders.haystack.kinesis.client.endpoint.service-endpoint=" + KINESIS_SERVICE_ENDPOINT
            );

            values.applyTo(context);
        }
    }
}
