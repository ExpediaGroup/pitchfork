package com.hotels.service.tracing.zipkintohaystack;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import static com.amazonaws.services.kinesis.model.ShardIteratorType.TRIM_HORIZON;

import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.localstack.LocalStackContainer;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorResult;
import com.amazonaws.services.kinesis.model.Record;
import com.expedia.open.tracing.Span;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HaystackKinesisForwarderTest {

    private static LocalStackContainer kinesisContainer;
    private static AmazonKinesis kinesisClient;

    @LocalServerPort
    private int localServerPort;

    @BeforeClass
    public static void setup() {
        startKinesisContainer();

        kinesisClient = setupKinesisClient();

        // create
        kinesisClient.createStream("proto-spans", 1);
    }

    private static void startKinesisContainer() {
        kinesisContainer = new LocalStackContainer().withServices(KINESIS);
        kinesisContainer.start();

        System.setProperty("pitchfork.ingress.rabbitmq.enabled", String.valueOf(false));
        System.setProperty("pitchfork.forwarders.haystack.kinesis.enabled", String.valueOf(true));
        System.setProperty("pitchfork.forwarders.haystack.kinesis.authentication-type", "BASIC");

        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                kinesisContainer.getEndpointConfiguration(KINESIS).getServiceEndpoint(), "us-west-1");
        System.setProperty("pitchfork.forwarders.haystack.kinesis.service-endpoint", endpointConfiguration.getServiceEndpoint());

        // https://github.com/localstack/localstack/blob/e479afa41df908305c4177276237925accc77e10/localstack/ext/java/src/test/java/cloud/localstack/BasicFunctionalityTest.java#L54
        System.setProperty("com.amazonaws.sdk.disableCbor", "true");
    }

    @Test
    public void shouldForwardTracesToKinesis() throws Exception {
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

        DescribeStreamResult streamResult = kinesisClient.describeStream("proto-spans");
        GetShardIteratorRequest shardIteratorRequest = new GetShardIteratorRequest()
                .withShardIteratorType(TRIM_HORIZON)
                .withShardId(streamResult.getStreamDescription().getShards().iterator().next().getShardId()) // there's just 1 shard
                .withStreamName("proto-spans");

        GetShardIteratorResult shardIterator = kinesisClient.getShardIterator(shardIteratorRequest);
        GetRecordsRequest getRecordsRequest = new GetRecordsRequest().withShardIterator(shardIterator.getShardIterator());

        // proxy is async, and kafka is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {
            GetRecordsResult records = kinesisClient.getRecords(getRecordsRequest);

            assertFalse(records.getRecords().isEmpty());

            Record record = records.getRecords().iterator().next(); // there's only one element so get first

            Optional<Span> span = deserialize(record.getData().array());

            assertTrue(span.isPresent());
            assertEquals(span.get().getTraceId(), traceId);
            assertEquals(span.get().getSpanId(), spanId);
            assertEquals(span.get().getParentSpanId(), parentId);
            assertEquals(span.get().getStartTime(), timestamp);
            assertEquals(span.get().getDuration(), duration);
        });
    }

    /**
     * Create consumer and subscribe to spans topic.
     */
    private static AmazonKinesis setupKinesisClient() {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                kinesisContainer.getEndpointConfiguration(KINESIS).getServiceEndpoint(), "us-west-1");

        return AmazonKinesisClientBuilder.standard()
                .withCredentials(kinesisContainer.getDefaultCredentialsProvider())
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    public static Optional<Span> deserialize(byte[] data) {
        try {
            return ofNullable(Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
        }
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
}
