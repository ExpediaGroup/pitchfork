package com.hotels.service.tracing.zipkintohaystack;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.awaitility.Awaitility.await;
import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.junit.Assert.assertEquals;

import static zipkin2.codec.SpanBytesEncoder.JSON_V1;
import static zipkin2.codec.SpanBytesEncoder.JSON_V2;

import java.util.List;

import javax.annotation.PostConstruct;

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
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import zipkin2.Endpoint;
import zipkin2.proto3.ListOfSpans;
import zipkin2.proto3.Span;

@DirtiesContext
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ZipkinForwarderTest {

    private static Integer ZIPKIN_PORT;

    @Autowired
    private TestRestTemplate restTemplate;

    @PostConstruct
    public void registerExtraMessageConverters() {
        this.restTemplate.getRestTemplate().getMessageConverters().add(new ProtobufHttpMessageConverter());
    }

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
        assertEquals("Expected a 200 status from pitchfork", HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(30, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertEquals("[\"abc\"]", responseFromZipkin.getBody());
        });
    }

    @Test
    public void shouldAcceptJsonV1AndForwardToZipkin() {
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
        assertEquals("Expected a 200 status from pitchfork", HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(30, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertEquals("[\"abc\"]", responseFromZipkin.getBody());
        });
    }

    @Test
    public void shouldAcceptProtoAndForwardToZipkin() throws Exception {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "abc";

        ListOfSpans listOfSpans = ListOfSpans.newBuilder().addSpans(
                Span.newBuilder()
                        .setId(ByteString.copyFrom(decodeHex(spanId.toCharArray())))
                        .setTraceId(ByteString.copyFrom(decodeHex(traceId.toCharArray())))
                        .setParentId(ByteString.copyFrom(decodeHex(parentId.toCharArray())))
                        .setTimestamp(timestamp)
                        .setDuration(duration)
                        .setLocalEndpoint(zipkin2.proto3.Endpoint.newBuilder()
                                .setServiceName(localEndpoint)
                                .build())
                        .build()
        ).build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-protobuf");
        HttpEntity<Message> request = new HttpEntity<>(listOfSpans, headers);

        ResponseEntity<String> responseFromVictim = this.restTemplate.postForEntity("/api/v2/spans", request, String.class);
        assertEquals("Expected a 200 status from pitchfork", HttpStatus.OK, responseFromVictim.getStatusCode());

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        await().atMost(30, SECONDS).untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity("http://localhost:" + ZIPKIN_PORT + "/api/v2/services", String.class);

            assertEquals(HttpStatus.OK, responseFromZipkin.getStatusCode());
            assertEquals("[\"abc\"]", responseFromZipkin.getBody());
        });
    }
}
