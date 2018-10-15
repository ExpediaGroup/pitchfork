package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HaystackDomainConverterTest {

    @Test
    public void shouldCreateHaystackSpanFromZipkinSpan() {
        String name = "pitchfork";
        String traceId = zipkinTraceId(123L);
        String parentId = zipkinSpanId(456L);
        String spanId = zipkinSpanId(789L);
        long timestamp = System.currentTimeMillis();
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
                .putTag("tag1", "value1")
                .putTag("tag2", "value2")
                .build();

        Span haystackSpan = HaystackDomainConverter.fromZipkinV2(zipkinSpan);

        assertEquals(traceId, haystackSpan.getTraceId());
        assertEquals(spanId, haystackSpan.getSpanId());
        assertEquals(parentId, haystackSpan.getParentSpanId());
        assertEquals(name, haystackSpan.getOperationName());
        assertEquals(timestamp, haystackSpan.getStartTime());
        assertEquals(duration, haystackSpan.getDuration());
        assertEquals(tags, haystackSpan.getTagsList().stream().collect(Collectors.toMap(Tag::getKey, Tag::getVStr)));
    }

    @Test
    public void shouldConvertZipkinErrorTagIntoSeparateHaystackErrorTags() {
        String errorMessage = "failure";

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .putTag("error", errorMessage)
                .build();

        Span haystackSpan = HaystackDomainConverter.fromZipkinV2(zipkinSpan);

        assertTrue(haystackSpan.getTagsList().stream()
                .filter(tag -> "error".equals(tag.getKey()))
                .map(Tag::getVBool)
                .findFirst()
                .orElse(false));

        assertEquals(errorMessage, haystackSpan.getTagsList().stream()
                .filter(tag -> "error_msg".equals(tag.getKey()))
                .map(Tag::getVStr)
                .findFirst()
                .orElse(null));
    }

    /**
     * Zipkin trace ids are 64 or 128 bits represented as 16 or 32 hex characters with '0' left padding
     */
    private String zipkinTraceId(long id) {
        return String.format("%032x", id);
    }

    /**
     * Zipkin span ids are 64 represented as 16 hex characters with '0' left padding
     */
    private String zipkinSpanId(long id) {
        return String.format("%016x", id);
    }
}