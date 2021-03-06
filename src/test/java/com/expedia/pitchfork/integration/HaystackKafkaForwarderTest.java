package com.expedia.pitchfork.integration;

import com.expedia.open.tracing.Span;
import com.expedia.pitchfork.Application;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
@DirtiesContext
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {HaystackKafkaForwarderTest.Initializer.class})
class HaystackKafkaForwarderTest {

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag("5.4.3"));

    @LocalServerPort
    private int localServerPort;

    private static Optional<Span> deserialize(byte[] data) {
        try {
            return ofNullable(Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
        }
    }

    @Test
    void shouldForwardTracesToKafka() throws Exception {
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
        try (KafkaConsumer<String, byte[]> consumer = setupConsumer()) {
            TestUtils.AWAIT.untilAsserted(() -> {
                ConsumerRecords<String, byte[]> records = consumer.poll(ofSeconds(1));

                assertThat(records).isNotEmpty();

                Optional<Span> span = deserialize(records.iterator().next().value()); // there's only one element so get first

                assertThat(span).isPresent();
                assertThat(span.get().getTraceId()).isEqualTo(traceId);
                assertThat(span.get().getSpanId()).isEqualTo(spanId);
                assertThat(span.get().getParentSpanId()).isEqualTo(parentId);
                assertThat(span.get().getStartTime()).isEqualTo(timestamp);
                assertThat(span.get().getDuration()).isEqualTo(duration);
            });
        }
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
                    "pitchfork.forwarders.haystack.kafka.enabled=true",
                    "pitchfork.forwarders.haystack.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()
            );
            values.applyTo(context);
        }
    }
}
