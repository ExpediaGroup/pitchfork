package com.hotels.service.tracing.zipkintohaystack;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.expedia.open.tracing.Span;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.kafka.KafkaSender;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {KafkaIngressTest.Initializer.class})
public class KafkaIngressTest {

    @Container
    private static KafkaContainer kafkaContainer = new KafkaContainer();

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            var values = TestPropertyValues.of(
                    "pitchfork.ingress.kafka.enabled=true",
                    "pitchfork.ingress.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers(),
                    "pitchfork.ingress.kafka.source-format=PROTO3",
                    "pitchfork.forwarders.haystack.kafka.enabled=true",
                    "pitchfork.forwarders.haystack.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()
            );
            values.applyTo(context);
        }
    }

    @Test
    public void shouldForwardTracesToKafka() throws Exception {
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

        // proxy is async, and kafka is async too, so we retry our assertions until they are true
        KafkaConsumer<String, byte[]> consumer = setupConsumer();

        await().atMost(10, SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, byte[]> records = consumer.poll(ofSeconds(1));

            assertFalse(records.isEmpty());

            Optional<Span> span = deserialize(records.iterator().next().value()); // there's only one element so get first

            assertTrue(span.isPresent());
            assertEquals(span.get().getTraceId(), traceId);
            assertEquals(span.get().getSpanId(), spanId);
            assertEquals(span.get().getParentSpanId(), parentId);
            assertEquals(span.get().getStartTime(), timestamp);
            assertEquals(span.get().getDuration(), duration);
        });
    }

    /**
     * Create reporter.
     */
    private static AsyncReporter<zipkin2.Span> setupReporter() {
        var sender = KafkaSender.newBuilder()
                .encoding(Encoding.PROTO3)
                .bootstrapServers(kafkaContainer.getBootstrapServers())
                .build();
        return AsyncReporter.create(sender);
    }

    /**
     * Create consumer and subscribe to spans topic.
     */
    private KafkaConsumer<String, byte[]> setupConsumer() {
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(
                ImmutableMap.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new ByteArrayDeserializer()
        );
        consumer.subscribe(singletonList("proto-spans"));

        return consumer;
    }

    /**
     * Create an admin client for Kafka.
     */
    private static AdminClient setupKafkaAdminClient() {
        return AdminClient.create(ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        ));
    }

    public static Optional<Span> deserialize(byte[] data) {
        try {
            return ofNullable(Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
        }
    }
}
