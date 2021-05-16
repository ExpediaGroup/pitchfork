package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog;

import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogSpan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatadogDomainConverterTest {

    @Test
    public void shouldCreateDatadogSpanFromZipkinSpan() {
        String name = "pitchfork";
        String traceId = "7b"; // 123 decimal
        String parentId = "1c8"; // 456 decimal
        String spanId = "315"; // 789 decimal
        long timestamp = 1621233762447L;
        long duration = 100L;

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(traceId)
                .id(spanId)
                .parentId(parentId)
                .name(name)
                .timestamp(timestamp)
                .duration(duration)
                .putTag("tag1", "value1")
                .putTag("tag2", "value2")
                .build();

        DatadogSpan datadogSpan = DatadogDomainConverter.fromZipkinV2(zipkinSpan);

        assertThat(datadogSpan.traceId()).isEqualTo(123L);
        assertThat(datadogSpan.spanId()).isEqualTo(789);
        assertThat(datadogSpan.parentId()).isEqualTo(456L);
        assertThat(datadogSpan.name()).isEqualTo(name);
        assertThat(datadogSpan.duration()).isEqualTo(100_000_000);
        assertThat(datadogSpan.start()).isEqualTo(1621233762447000000L);

        // No error
        assertThat(datadogSpan.error()).isNull();

        // 2 user defined tags
        assertThat(datadogSpan.meta()).hasSize(2);
        assertThat(datadogSpan.meta().get("tag1")).isEqualTo("value1");
        assertThat(datadogSpan.meta().get("tag2")).isEqualTo("value2");
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
