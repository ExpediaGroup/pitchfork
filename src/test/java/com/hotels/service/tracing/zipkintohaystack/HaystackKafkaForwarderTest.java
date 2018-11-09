package com.hotels.service.tracing.zipkintohaystack;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static com.hotels.service.tracing.zipkintohaystack.utils.TestHelpers.retryUntilSuccess;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.expedia.open.tracing.Span;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import zipkin2.Endpoint;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HaystackKafkaForwarderTest {

    private static KafkaContainer kafkaContainer;

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @BeforeClass
    public static void setup() {
        startKafkaContainer();
    }

    private static void startKafkaContainer() {
        kafkaContainer = new KafkaContainer();
        kafkaContainer.start();

        System.setProperty("pitchfork.forwarders.haystack.kafka.enabled", String.valueOf(true));
        System.setProperty("pitchfork.forwarders.haystack.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
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

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(List.of(zipkinSpan)), headers);

        ResponseEntity<String> responseFromVictim = this.restTemplate.postForEntity("/api/v2/spans", request, String.class);
        assertEquals("Expected a 200 status from pitchfork", HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and kafka is async too, so we retry our assertions until they are true
        KafkaConsumer<String, byte[]> consumer = setupConsumer();

        retryUntilSuccess(Duration.ofSeconds(10), () -> {
            ConsumerRecords<String, byte[]> records = consumer.poll(100);

            assertTrue(!records.isEmpty());

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

    public static Optional<Span> deserialize(byte[] data) {
        try {
            return ofNullable(Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
        }
    }
}
