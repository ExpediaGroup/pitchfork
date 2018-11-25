package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.expedia.open.tracing.Span;
import com.expedia.open.tracing.Tag;

public class HaystackDomainConverterTest {

    private static final boolean ACCEPT_NULL_TIMESTAMPS = true;
    private static final boolean REJECT_NULL_TIMESTAMPS = false;
    private static final int MAX_DRIFT_FOR_TIMESTAMPS_DISABLED = -1;

    private static HaystackDomainConverter victim;

    @BeforeAll
    public static void setup() {
        victim = new HaystackDomainConverter(ACCEPT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED);
    }

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
                .putTag("tag1", "value1")
                .putTag("tag2", "value2")
                .build();

        Optional<Span> haystackSpan = victim.fromZipkinV2(zipkinSpan);

        assertTrue(haystackSpan.isPresent());
        assertEquals(traceId, haystackSpan.get().getTraceId());
        assertEquals(spanId, haystackSpan.get().getSpanId());
        assertEquals(parentId, haystackSpan.get().getParentSpanId());
        assertEquals(name, haystackSpan.get().getOperationName());
        assertEquals(timestamp, haystackSpan.get().getStartTime());
        assertEquals(duration, haystackSpan.get().getDuration());
        assertEquals(tags, haystackSpan.get().getTagsList().stream().collect(Collectors.toMap(Tag::getKey, Tag::getVStr)));
    }

    @Test
    public void shouldConvertZipkinErrorTagIntoSeparateHaystackErrorTags() {
        String errorMessage = "failure";

        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .putTag("error", errorMessage)
                .build();

        Optional<Span> haystackSpan = victim.fromZipkinV2(zipkinSpan);

        assertTrue(haystackSpan.isPresent());

        assertTrue(haystackSpan.get().getTagsList().stream()
                .filter(tag -> "error".equals(tag.getKey()))
                .map(Tag::getVBool)
                .findFirst()
                .orElse(false));

        assertEquals(errorMessage, haystackSpan.get().getTagsList().stream()
                .filter(tag -> "error_msg".equals(tag.getKey()))
                .map(Tag::getVStr)
                .findFirst()
                .orElse(null));
    }

    @ParameterizedTest
    @MethodSource("timestamps")
    public void shouldDiscardSpanIfTimestampIsInvalid(boolean rejectNullTimestamps, int maxDrift, Long timestamp, boolean spanIsKept) {
        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .timestamp(timestamp)
                .build();

        var victim = new HaystackDomainConverter(rejectNullTimestamps, maxDrift);
        Optional<Span> haystackSpan = victim.fromZipkinV2(zipkinSpan);

        assertEquals(haystackSpan.isPresent(), spanIsKept);
    }

    private static Stream<Arguments> timestamps() {
        return Stream.of(
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, false), // reject null timestamp
                Arguments.of(ACCEPT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, true), // accept null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, 123L, true), // accept non null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() - SECONDS.toMicros(10), false), // reject span if too old
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() + SECONDS.toMicros(10), false), // reject span if too recent
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() + SECONDS.toMicros(5), true), // accept if only 5 sec in the future
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() - SECONDS.toMicros(5), true) // accept if only 5 sec in the past
        );
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