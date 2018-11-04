package com.hotels.service.tracing.zipkintohaystack;

import static org.junit.Assert.assertEquals;

import static com.hotels.service.tracing.zipkintohaystack.TestHelpers.retryUntilSuccess;
import static zipkin2.codec.SpanBytesEncoder.JSON_V1;
import static zipkin2.codec.SpanBytesEncoder.JSON_V2;

import java.time.Duration;
import java.util.List;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import zipkin2.Endpoint;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ZipkinForwarderTest {

    private static Integer ZIPKIN_PORT;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setup() {
        startZipkinContainer();
    }

    private static void startZipkinContainer() {
        GenericContainer zipkinContainer = new GenericContainer("openzipkin/zipkin:2.11")
                .withExposedPorts(9411)
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*started on port.*\\s"));
        zipkinContainer.start();

        ZIPKIN_PORT = zipkinContainer.getMappedPort(9411);

        System.setProperty("pitchfork.forwarders.zipkin.enabled", String.valueOf(true));
        System.setProperty("pitchfork.forwarders.zipkin.endpoint", "http://localhost:" + ZIPKIN_PORT + "/api/v2/spans");
    }

    @Test
    public void shouldAcceptJsonV2AndForwardToZipkin() throws Exception {
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

        byte[] bytes = JSON_V2.encodeList(List.of(zipkinSpan));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(new String(bytes), headers);

        ResponseEntity<String> responseFromVictim = this.restTemplate.postForEntity("/api/v2/spans", request, String.class);
        assertEquals(HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        retryUntilSuccess(Duration.ofSeconds(30), () -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertEquals("[\"abc\"]", responseFromZipkin.getBody());
        });
    }

    @Test
    public void shouldAcceptJsonV1AndForwardToZipkin() throws Exception {
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

        byte[] bytes = JSON_V1.encodeList(List.of(zipkinSpan));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(new String(bytes), headers);

        ResponseEntity<String> responseFromVictim = this.restTemplate.postForEntity("/api/v1/spans", request, String.class);
        assertEquals(HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        retryUntilSuccess(Duration.ofSeconds(30), () -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertEquals("[\"abc\"]", responseFromZipkin.getBody());
        });
    }
}
