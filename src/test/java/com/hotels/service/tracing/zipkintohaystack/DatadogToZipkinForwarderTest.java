package com.hotels.service.tracing.zipkintohaystack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.hotels.service.tracing.zipkintohaystack.TestUtils.AWAIT;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {DatadogToZipkinForwarderTest.Initializer.class})
class DatadogToZipkinForwarderTest {

    @Container
    private static final GenericContainer zipkinContainer = new GenericContainer("openzipkin/zipkin:2.23")
            .withExposedPorts(9411)
            .waitingFor(new HttpWaitStrategy().forPath("/health"));
    @LocalServerPort
    private int localServerPort;
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAcceptDatadogFormatAndForwardToZipkin() {
        String spanId = "2696599e12b2a265";
        String traceId = "3116bae014149aad";
        String parentId = "d6318b5dfa0088fa";
        long timestamp = 1528386023537760L;
        int duration = 17636;
        String localEndpoint = "jsonv2";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var url = "http://localhost:" + localServerPort + "/v0.3/traces";
        String datadogSpan = """
                [
                  [
                    {
                      "duration": 12345,
                      "name": "span_name",
                      "resource": "/home",
                      "service": "service_name",
                      "span_id": 987654321,
                      "start": 0,
                      "trace_id": 123456789
                    }
                  ]
                ]
                """;

        // build the request
        HttpEntity<String> entity = new HttpEntity<>(datadogSpan, headers);

        restTemplate.put(url, entity);

        // proxy is async, and zipkin is async too, so we retry our assertions until they are true
        AWAIT.untilAsserted(() -> {

            // assert that traces were forwarded to zipkin by asking which services it knows about
            ResponseEntity<String> responseFromZipkin = restTemplate
                    .getForEntity(
                            "http://" + zipkinContainer.getContainerIpAddress() + ":" + zipkinContainer.getFirstMappedPort() + "/api/v2/services",
                            String.class);

            assertThat(HttpStatus.OK).isEqualTo(responseFromZipkin.getStatusCode());
            assertThat(responseFromZipkin.getBody()).contains("\"service_name\"");
        });
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
