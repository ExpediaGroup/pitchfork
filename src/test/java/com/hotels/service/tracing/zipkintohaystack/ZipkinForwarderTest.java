package com.hotels.service.tracing.zipkintohaystack;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static zipkin2.codec.SpanBytesEncoder.JSON_V1;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ZipkinForwarderTest {

    private static Integer ZIPKIN_PORT;

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setup() {
        startZipkinContainer();
    }

    private static void startZipkinContainer() {
        GenericContainer zipkinContainer = new GenericContainer("openzipkin/zipkin:2.12")
                .withExposedPorts(9411)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Started ZipkinServer.*\\s"));
        zipkinContainer.start();

        ZIPKIN_PORT = zipkinContainer.getMappedPort(9411);

        System.setProperty("pitchfork.ingress.rabbitmq.enabled", String.valueOf(false));
        System.setProperty("pitchfork.forwarders.zipkin.http.enabled", String.valueOf(true));
        System.setProperty("pitchfork.forwarders.zipkin.http.endpoint", "http://localhost:" + ZIPKIN_PORT + "/api/v2/spans");
    }

    @Test
    public void shouldAcceptJsonV2AndForwardToZipkin() {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "jsonv2";

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

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertTrue(responseFromZipkin.getBody().contains("\"jsonv2\""));
        });
    }

    @Test
    public void shouldAcceptThriftAndForwardToZipkin() {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "thrift";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter(Encoding.THRIFT, false);
        reporter.report(zipkinSpan);

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertTrue(responseFromZipkin.getBody().contains("\"thrift\""));
        });
    }

    @Test
    public void shouldAcceptCompressedJsonV2AndForwardToZipkin() throws Exception {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "compressedjsonv2";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter(Encoding.JSON, true);
        reporter.report(zipkinSpan);

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertTrue(responseFromZipkin.getBody().contains("\"compressedjsonv2\""));
        });
    }

    @Test
    public void shouldAcceptJsonV1AndForwardToZipkin() {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "jsonv1";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        // We need to do our own reporting when using Json V1 as this has been deprecated and the standard zipkin reporter doesnt support this encoding any more
        byte[] bytes = JSON_V1.encodeList(List.of(zipkinSpan));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(new String(bytes), headers);
        ResponseEntity<String> responseFromVictim = this.restTemplate.postForEntity("/api/v1/spans", request, String.class);
        assertEquals("Expected a 200 status from pitchfork", HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertTrue(responseFromZipkin.getBody().contains("\"jsonv1\""));
        });
    }

    @Test
    public void shouldAcceptProtoAndForwardToZipkin() throws Exception {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "proto";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter(Encoding.PROTO3, false);
        reporter.report(zipkinSpan);

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(10, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertTrue(responseFromZipkin.getBody().contains("\"proto\""));
        });
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
