package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import static java.lang.System.currentTimeMillis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;

public class HaystackDomainConverterTest {

    @Test
    public void shouldCreateHaystackSpanFromZipkinSpan() {
        String name = "pitchfork";
        String traceId = zipkinTraceId(123L);
        String parentId = zipkinSpanId(456L);
        String spanId = zipkinSpanId(789L);
        long timestamp = currentTimeMillis();
        long duration = 100L;
        Map<String, String> tags = Map.of(
                "span.kind", "client",
                "tag1", "value1",
                "tag2", "value2"
        );

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(traceId)
                .id(spanId)
                .parentId(parentId)
                .name(name)
                .timestamp(timestamp)
                .duration(duration)
                .kind(zipkin2.Span.Kind.CLIENT)
                .addAnnotation(123L, "something")
                .addAnnotation(456L, "something-else")
                .putTag("tag1", "value1")
                .putTag("tag2", "value2")
                .build();

        Span haystackSpan = HaystackDomainConverter.fromZipkinV2(zipkinSpan);

        assertThat(haystackSpan.getTraceId()).isEqualTo(traceId);
        assertThat(haystackSpan.getSpanId()).isEqualTo(spanId);
        assertThat(haystackSpan.getParentSpanId()).isEqualTo(parentId);
        assertThat(haystackSpan.getOperationName()).isEqualTo(name);
        assertThat(haystackSpan.getDuration()).isEqualTo(duration);
        assertThat(haystackSpan.getStartTime()).isEqualTo(timestamp);

        // 2 user defined tags + span.kind
        assertThat(haystackSpan.getTagsList()).hasSize(3);
        assertThat(haystackSpan.getTagsList()).extracting("key").contains("tag1", "tag2", "span.kind");
        assertThat(haystackSpan.getTagsList()).extracting("vStr").contains("value1", "value2", "client");

        // 2 logs or annotations
        assertThat(haystackSpan.getLogsList()).hasSize(2);
        assertThat(haystackSpan.getLogsList()).extracting("timestamp").contains(123L, 456L);
    }

    @Test
    public void shouldConvertZipkinErrorTagIntoSeparateHaystackErrorTags() {
        String errorMessage = "failure_msg";

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .putTag("error", "failure_msg")
                .build();

        Span haystackSpan = HaystackDomainConverter.fromZipkinV2(zipkinSpan);

        var errorTag = haystackSpan.getTagsList().stream()
                .filter(tag -> "error".equals(tag.getKey()))
                .map(Tag::getVBool)
                .findFirst();
        assertThat(errorTag).isPresent();
        assertThat(errorTag.get()).isTrue();

        var errorMessageTag = haystackSpan.getTagsList().stream()
                .filter(tag -> "error_msg".equals(tag.getKey()))
                .map(Tag::getVStr)
                .findFirst();

        assertThat(errorMessageTag).isPresent();
        assertThat(errorMessageTag.get()).isEqualTo(errorMessage);
    }

    /**
     * Zipkin trace ids are 64 or 128 bits represented as 16 or 32 hex characters with '0' left padding
     */
    private String zipkinTraceId(long id) {
        return String.format("%016x", id);
    }

    /**
     * Zipkin span ids are 64 represented as 16 hex characters with '0' left padding
     */
    private String zipkinSpanId(long id) {
        return String.format("%016x", id);
    }
}
