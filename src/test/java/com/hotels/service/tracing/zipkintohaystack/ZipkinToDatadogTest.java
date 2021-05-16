package com.hotels.service.tracing.zipkintohaystack;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.RequestDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import zipkin2.Endpoint;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import static com.hotels.service.tracing.zipkintohaystack.TestUtils.AWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {ZipkinToDatadogTest.Initializer.class})
class ZipkinToDatadogTest {

    @Container
    private static final MockServerContainer datadogContainer = new MockServerContainer("5.11.2");
    @LocalServerPort
    private int localServerPort;

    @Test
    void shouldAcceptZipkinTracesAndForwardToDatadog() throws Exception {
        MockServerClient mockServerClient = new MockServerClient(datadogContainer.getHost(), datadogContainer.getServerPort());

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/v0.3/traces"))
                .respond(response().withStatusCode(200));

        String spanId = "352bff9a74ca9ad2";
        String traceId = "5af7183fb1d4cf5f";
        String parentId = "6b221d5bc9e6496c";
        long timestamp = System.currentTimeMillis();
        int duration = 17636;
        String localEndpoint = "service_name";

        var zipkinSpan = zipkin2.Span.newBuilder()
                .id(spanId)
                .traceId(traceId)
                .putTag("foo", "bar")
                .addAnnotation(timestamp, "zoo")
                .parentId(parentId)
                .timestamp(timestamp)
                .duration(duration)
                .localEndpoint(Endpoint.newBuilder().serviceName(localEndpoint).build())
                .build();

        var reporter = setupReporter(Encoding.JSON, false);
        reporter.report(zipkinSpan);

        // we retry our assertions until they are true
        AWAIT.untilAsserted(() -> {
            RequestDefinition[] recordedRequests = mockServerClient.retrieveRecordedRequests(request()
                    .withPath("/v0.3/traces")
                    .withMethod("PUT"));

            assertThat(recordedRequests).hasSize(1);

            HttpRequest recordedRequest = (HttpRequest) recordedRequests[0];
            assertThat(recordedRequest.getBody().getValue().toString()).contains("6554734444506566495"); // this is the decimal representation of the trace id
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
                    "pitchfork.forwarders.datadog.enabled=true",
                    "pitchfork.forwarders.datadog.host=" + datadogContainer.getContainerIpAddress(),
                    "pitchfork.forwarders.datadog.port=" + datadogContainer.getFirstMappedPort()
            );
            values.applyTo(context);
        }
    }
}
