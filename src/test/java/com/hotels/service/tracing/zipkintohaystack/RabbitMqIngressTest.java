package com.hotels.service.tracing.zipkintohaystack;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.amqp.RabbitMQSender;

import java.time.Duration;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {RabbitMqIngressTest.Initializer.class})
class RabbitMqIngressTest {

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer();
    @Container
    private static final GenericContainer rabbitMqContainer = new GenericContainer("rabbitmq:3.7.14-alpine")
            .withExposedPorts(5672)
            .withNetworkAliases("rabbitmq")
            .waitingFor(new HostPortWaitStrategy());
    private static final ConditionFactory AWAIT = await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofSeconds(1))
            .pollDelay(Duration.ofSeconds(1));

    @BeforeAll
    static void setup() throws Exception {
        setupRabbitMqQueue();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            var values = TestPropertyValues.of(
                    "pitchfork.ingress.rabbitmq.enabled=true",
                    "pitchfork.ingress.rabbitmq.host=" + rabbitMqContainer.getContainerIpAddress(),
                    "pitchfork.ingress.rabbitmq.port=" + rabbitMqContainer.getFirstMappedPort(),
                    "pitchfork.ingress.rabbitmq.queue-name=zipkin",
                    "pitchfork.ingress.rabbitmq.source-format=PROTO3",
                    "pitchfork.forwarders.haystack.kafka.enabled=true",
                    "pitchfork.forwarders.haystack.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()
            );
            values.applyTo(context);
        }
    }

    @Test
    void shouldForwardTracesToKafka() {
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

        var reporter = setupReporter(Encoding.PROTO3);
        reporter.report(zipkinSpan);

        // proxy is async, and kafka is async too, so we retry our assertions until they are true
        try (KafkaConsumer<String, byte[]> consumer = setupConsumer()) {
            AWAIT.untilAsserted(() -> {
                ConsumerRecords<String, byte[]> records = consumer.poll(ofSeconds(1));

                assertThat(records).isNotEmpty();

                Optional<com.expedia.open.tracing.Span> span = deserialize(
                        records.iterator().next().value()); // there's only one element so get first

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
     * Create reporter.
     */
    private AsyncReporter<Span> setupReporter(Encoding encoding) {
        var sender = RabbitMQSender.newBuilder()
                .username("guest")
                .username("guest")
                .virtualHost("/")
                .encoding(encoding)
                .queue("zipkin")
                .addresses(rabbitMqContainer.getContainerIpAddress() + ":" + rabbitMqContainer.getFirstMappedPort())
                .build();
        return AsyncReporter.create(sender);
    }

    private static void setupRabbitMqQueue() throws Exception {
        var channel = getRabbitMqChannel();
        var exchangeName = "pitchforkExchange";
        var routingKey = "pitchforkExchange";
        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueDeclare("zipkin", true, false, true, null);
        channel.queueBind("zipkin", exchangeName, routingKey);
    }

    private static Channel getRabbitMqChannel() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost(rabbitMqContainer.getContainerIpAddress());
        factory.setPort(rabbitMqContainer.getFirstMappedPort());

        var connection = factory.newConnection();

        return connection.createChannel();
    }

    private static Optional<com.expedia.open.tracing.Span> deserialize(byte[] data) {
        try {
            return ofNullable(com.expedia.open.tracing.Span.parseFrom(data));
        } catch (Exception e) {
            fail("Failed to deserialise span from data");
            return empty();
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
}
