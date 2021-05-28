package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog;

import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogSpan;
import org.junit.jupiter.api.Test;
import zipkin2.Endpoint;
import zipkin2.Span;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class DatadogDomainConverterTest {

    @Test
    public void shouldCreateZipkinSpanFromDatadog() {
        String name = "name";
        String serviceName = "service_name";
        long traceId = 123L; // 7b hexadecimal
        long parentId = 456L; // 1c8 hexadecimal
        long spanId = 789L; // 315 hexadecimal
        long timestampMs = 1621233762447L;
        long durationMs = 150;

        DatadogSpan datadogSpan = new DatadogSpan(
                BigInteger.valueOf(traceId),
                BigInteger.valueOf(spanId),
                BigInteger.valueOf(parentId),
                MILLISECONDS.toNanos(timestampMs),
                MILLISECONDS.toNanos(durationMs),
                1,
                Map.of("tag1", "value1",
                        "tag2", "value2"),
                Collections.emptyMap(),
                name,
                null,
                serviceName,
                "web"
        );

        Span zipkinSpan = DatadogDomainConverter.toZipkin(datadogSpan);

        assertThat(zipkinSpan.traceId()).isEqualTo("000000000000007b");
        assertThat(zipkinSpan.id()).isEqualTo("0000000000000315");
        assertThat(zipkinSpan.parentId()).isEqualTo("00000000000001c8");
        assertThat(zipkinSpan.name()).isEqualTo(name);
        assertThat(zipkinSpan.tags().get("type")).isEqualTo("web");
        assertThat(zipkinSpan.localServiceName()).isEqualTo(serviceName);
        assertThat(zipkinSpan.duration()).isEqualTo(MILLISECONDS.toMicros(durationMs));
        assertThat(zipkinSpan.timestamp()).isEqualTo(MILLISECONDS.toMicros(timestampMs));

        // No error
        assertThat(zipkinSpan.tags().get("error")).isNotBlank();

        // 2 user defined tags
        assertThat(zipkinSpan.tags()).hasSize(4); // 2 tags + the error tag + the type tag
        assertThat(zipkinSpan.tags().get("tag1")).isEqualTo("value1");
        assertThat(zipkinSpan.tags().get("tag2")).isEqualTo("value2");
    }

    @Test
    public void shouldConvertZipkinToDatadog() {
        String name = "name";
        String serviceName = "service_name";
        String traceId = "7b"; // 123 decimal
        String parentId = "1c8"; // 456 decimal
        String spanId = "315"; // 789 decimal
        var kind = Span.Kind.CLIENT;
        long timestampMs = 1621233762447L;
        long durationMs = 150;

        zipkin2.Span zipkinSpan = Span.newBuilder()
                .traceId(traceId)
                .id(spanId)
                .parentId(parentId)
                .name(name)
                .localEndpoint(Endpoint.newBuilder().serviceName(serviceName).build())
                .timestamp(MILLISECONDS.toMicros(timestampMs))
                .duration(MILLISECONDS.toMicros(durationMs))
                .kind(kind)
                .putTag("tag1", "value1")
                .putTag("tag2", "value2")
                .build();

        DatadogSpan datadogSpan = DatadogDomainConverter.fromZipkinV2(zipkinSpan);

        assertThat(datadogSpan.meta().get("span.kind")).isEqualTo(kind.name());
        assertThat(datadogSpan.traceId()).isEqualTo(123L);
        assertThat(datadogSpan.spanId()).isEqualTo(789);
        assertThat(datadogSpan.parentId()).isEqualTo(456L);
        assertThat(datadogSpan.name()).isEqualTo(name);
        assertThat(datadogSpan.service()).isEqualTo(serviceName);
        assertThat(datadogSpan.start()).isEqualTo(MILLISECONDS.toNanos(timestampMs));
        assertThat(datadogSpan.duration()).isEqualTo(MILLISECONDS.toNanos(durationMs));

        // No error
        assertThat(datadogSpan.error()).isNull();

    }

    @Test
    public void shouldTruncateLongIds() {
        String name = "pitchfork";
        String traceId = "352bff9a74ca9ad25af7183fb1d4cf5f"; // 5af7183fb1d4cf5f (rightmost part) which is 6554734444506566495 decimal
        String spanId = "20471a"; // 2115354 decimal

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(traceId)
                .id(spanId)
                .name(name)
                .build();

        DatadogSpan datadogSpan = DatadogDomainConverter.fromZipkinV2(zipkinSpan);

        assertThat(datadogSpan.traceId()).isEqualTo(6554734444506566495L);
        assertThat(datadogSpan.spanId()).isEqualTo(2115354);
    }

    @Test
    public void shouldConvertZipkinErrorTag() {
        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId("7b")
                .id("1c8")
                .putTag("error", "failure_msg")
                .build();

        DatadogSpan datadogSpan = DatadogDomainConverter.fromZipkinV2(zipkinSpan);

        assertThat(datadogSpan.error()).isEqualTo(1); // 1 = error
    }
}
