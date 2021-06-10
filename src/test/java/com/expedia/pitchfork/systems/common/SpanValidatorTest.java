package com.expedia.pitchfork.systems.common;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.stream.Stream;

import com.expedia.pitchfork.monitoring.metrics.MetersProvider;
import com.expedia.pitchfork.systems.common.SpanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.micrometer.core.instrument.Counter;
import zipkin2.Endpoint;
import zipkin2.Span;

public class SpanValidatorTest {

    private static final boolean ACCEPT_NULL_TIMESTAMPS = true;
    private static final boolean REJECT_NULL_TIMESTAMPS = false;
    private static final int MAX_DRIFT_FOR_TIMESTAMPS_DISABLED = -1;
    private static final boolean DISABLE_NULL_TIMESTAMPS_VALIDATION = false;
    private static final int DISABLE_TIMESTAMP_DRIFT_VALIDATION = -1;
    private static final String[] DISABLE_INVALID_SERVICE_NAMES = new String[]{};
    private final MetersProvider metersProvider = mock(MetersProvider.class);
    private final Counter mockCounter = mock(Counter.class);

    @BeforeEach
    public void setup() {
        initMocks(this);

        when(metersProvider.getInvalidSpansCounter()).thenReturn(mockCounter);
    }

    @ParameterizedTest
    @MethodSource("timestamps")
    public void shouldDiscardSpanIfTimestampIsInvalid(boolean rejectNullTimestamps, int maxDrift, Long timestamp, boolean spanIsKept) {
        zipkin2.Span zipkinSpan = Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .localEndpoint(Endpoint.newBuilder().serviceName("unknown").build())
                .timestamp(timestamp != null ? timestamp * 1000 : null) // millis to micros
                .build();

        var victim = new SpanValidator(rejectNullTimestamps, maxDrift, DISABLE_INVALID_SERVICE_NAMES, metersProvider);
        victim.initialize();

        boolean isSpanValid = victim.isSpanValid(zipkinSpan);

        assertEquals(isSpanValid, spanIsKept);
    }

    @ParameterizedTest
    @MethodSource("serviceNames")
    public void shouldDiscardSpanIfServiceNameIsInvalid(boolean spanIsKept, String serviceName, String[] invalidServiceNames) {
        zipkin2.Span zipkinSpan = Span.newBuilder()
                .traceId(zipkinTraceId(123L))
                .id(zipkinSpanId(456L))
                .localEndpoint(Endpoint.newBuilder().serviceName(serviceName).build())
                .timestamp(System.currentTimeMillis())
                .build();

        var victim = new SpanValidator(DISABLE_NULL_TIMESTAMPS_VALIDATION, DISABLE_TIMESTAMP_DRIFT_VALIDATION, invalidServiceNames, metersProvider);
        victim.initialize();

        boolean isSpanValid = victim.isSpanValid(zipkinSpan);

        assertEquals(isSpanValid, spanIsKept);
    }

    private static Stream<Arguments> serviceNames() {
        return Stream.of(
                Arguments.of(true, "unknown", new String[]{""}), // accept if invalid service names list is empty
                Arguments.of(false, "unknown", new String[]{"unknown"}) // accept if service name is in the list of invalid names
        );
    }

    private static Stream<Arguments> timestamps() {
        return Stream.of(
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, false), // reject null timestamp
                Arguments.of(ACCEPT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, null, true), // accept null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, MAX_DRIFT_FOR_TIMESTAMPS_DISABLED, 123L, true), // accept non null timestamp
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() - SECONDS.toMillis(10), false), // reject span if too old
                Arguments.of(REJECT_NULL_TIMESTAMPS, 5, currentTimeMillis() + SECONDS.toMillis(10), false), // reject span if too recent
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() + SECONDS.toMillis(5), true), // accept if only 5 sec in the future
                Arguments.of(REJECT_NULL_TIMESTAMPS, 10, currentTimeMillis() - SECONDS.toMillis(5), true)
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
