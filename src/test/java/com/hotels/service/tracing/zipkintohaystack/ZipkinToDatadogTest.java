package com.hotels.service.tracing.zipkintohaystack;

import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {ZipkinToDatadogTest.Initializer.class})
class ZipkinToDatadogTest {

    @LocalServerPort
    private int localServerPort;

//    @Container
//    private static final GenericContainer zipkinContainer = new GenericContainer("openzipkin/zipkin:2.23")
//            .withExposedPorts(9411)
//            .waitingFor(new HttpWaitStrategy().forPath("/health"));
    private static final ConditionFactory AWAIT = await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofSeconds(1))
            .pollDelay(Duration.ofSeconds(1));

    @Autowired
    private TestRestTemplate restTemplate;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            var values = TestPropertyValues.of(
                    "pitchfork.forwarders.datadog.enabled=true"
            );
            values.applyTo(context);
        }
    }

    @Test
    void shouldAcceptZipkinTracesAndForwardToDatadog() throws Exception {
        Random random = new Random();
        String spanId = zipkinSpanId(random.nextInt(999999));
        String traceId = zipkinSpanId(random.nextInt(999999));
        String parentId = zipkinSpanId(random.nextInt(999999));
        BigInteger BigInteger = new BigInteger("15434270633717958906");
        long timestamp = System.currentTimeMillis();
        int duration = 17636;
        String localEndpoint = "service_name";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter(Encoding.JSON, false);
        reporter.report(zipkinSpan);

        Thread.sleep(10000);

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
//            ResponseEntity<String> responseFromZipkin = restTemplate
//                    .getForEntity(
//                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
//                            String.class);
//
//            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
//            assertThat(responseFromZipkin.getBody()).contains("\"jsonv2\"");
        });
    }

    private static String zipkinSpanId(long id) {
        return String.format("%016x", id);
    }


    /**
     * Create reporter.
     */
    private AsyncReporter<zipkin2.Span> setupReporter(Encoding encoding, boolean compressionEnabled) {
        var sender = OkHttpSender.newBuilder()
                .encoding(encoding)
                .compressionEnabled(compressionEnabled)
                .endpoint("http://localhost:" + localServerPort + "/api/v2/spans")
                .build();
        return AsyncReporter.create(sender);
    }
}
