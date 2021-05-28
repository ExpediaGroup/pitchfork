package com.expedia.pitchfork.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static zipkin2.codec.SpanBytesEncoder.JSON_V1;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {ZipkinForwarderTest.Initializer.class})
class ZipkinForwarderTest {

    @Container
    private static final GenericContainer zipkinContainer = new GenericContainer("openzipkin/zipkin:2.23")
            .withExposedPorts(9411)
            .waitingFor(new HttpWaitStrategy().forPath("/health"));
    @LocalServerPort
    private int localServerPort;
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAcceptJsonV2AndForwardToZipkin() {
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
        TestUtils.AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"jsonv2\"");
        });
    }

    @Test
    void shouldAcceptThriftAndForwardToZipkin() {
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
        TestUtils.AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"thrift\"");
        });
    }

    @Test
    void shouldAcceptCompressedJsonV2AndForwardToZipkin() throws Exception {
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
        TestUtils.AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"compressedjsonv2\"");
        });
    }

    @Test
    void shouldAcceptJsonV1AndForwardToZipkin() {
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
        assertThat(HttpStatus.OK).isEqualTo(responseFromVictim.getStatusCode()).withFailMessage("Expected a 200 status from pitchfork");

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        TestUtils.AWAIT.untilAsserted(() -> {
            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"jsonv1\"");
        });
    }

    @Test
    void shouldAcceptProtoAndForwardToZipkin() throws Exception {
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
        TestUtils.AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"proto\"");
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

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext context) {
            var values = TestPropertyValues.of(
                    "pitchfork.forwarders.zipkin.http.enabled=true",
                    "pitchfork.forwarders.zipkin.http.endpoint=http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer
                            .getFirstMappedPort() + "/api/v2/spans"
            );
            values.applyTo(context);
        }
    }
}
