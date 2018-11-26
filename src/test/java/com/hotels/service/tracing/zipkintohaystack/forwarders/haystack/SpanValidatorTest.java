package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SpanValidatorTest {

    private static final boolean ACCEPT_NULL_TIMESTAMPS = true;
    private static final boolean REJECT_NULL_TIMESTAMPS = false;
    private static final int MAX_DRIFT_FOR_TIMESTAMPS_DISABLED = -1;

    @ParameterizedTest
    @MethodSource("timestamps")
    public void shouldDiscardSpanIfTimestampIsInvalid(boolean rejectNullTimestamps, int maxDrift, Long timestamp, boolean spanIsKept) {
        zipkin2.Span zipkinSpan = zipkin2.Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .timestamp(timestamp != null ? timestamp * 1000 : null) // millis to micros
                .build();

        var victim = new SpanValidator(rejectNullTimestamps, maxDrift);
        boolean isSpanValid = victim.isSpanValid(zipkinSpan);

        assertEquals(isSpanValid, spanIsKept);
    }

    private static Stream<Arguments> timestamps() {
        return Stream.of(
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, false), // reject null timestamp
                Arguments.of(ACCEPT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, true), // accept null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, 123L, true), // accept non null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() - SECONDS.toMillis(10), false), // reject span if too old
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() + SECONDS.toMillis(10), false), // reject span if too recent
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() + SECONDS.toMillis(5), true), // accept if only 5 sec in the future
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() - SECONDS.toMillis(5), true) // accept if only 5 sec in the past
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