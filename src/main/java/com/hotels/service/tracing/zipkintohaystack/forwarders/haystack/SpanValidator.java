/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import com.hotels.service.tracing.zipkintohaystack.LogFormatEnforcer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validator for spans.
 * Please note that some or all checks are optional, for example, a span with a null timestamp is still a valid span.
 * These validations should be used if there's a need to identify or block bad actors.
 */
@Component
public class SpanValidator {

    private static final LogFormatEnforcer LOGGER = LogFormatEnforcer.loggerFor(SpanValidator.class);
    private static final int VALIDATION_DISABLED = -1;

    private final boolean acceptNullTimestamps;
    private final int maxTimestampDriftSeconds;

    public SpanValidator(@Value("${pitchfork.validators.accept-null-timestamps}") boolean acceptNullTimestamps,
                         @Value("${pitchfork.validators.max-timestamp-drift-seconds}") int maxTimestampDriftSeconds) {
        this.acceptNullTimestamps = acceptNullTimestamps;
        this.maxTimestampDriftSeconds = maxTimestampDriftSeconds;
    }

    // TODO: add metrics with discarded/invalid spans
    public boolean isSpanValid(zipkin2.Span span) {
        if (span.traceId() == null) {
            LOGGER.error(message -> message.operation("isSpanValid").msg("null traceId")
                    .spanId(span::id).and("service", span::localServiceName));
            return false;
        }

        if (span.timestamp() == null && !acceptNullTimestamps) {
            LOGGER.error(message -> message.operation("isSpanValid").msg("null timestamp")
                    .spanId(span::id).and("traceId", span::traceId).and("service", span::localServiceName));
            return false;
        }

        if (span.timestamp() != null && maxTimestampDriftSeconds != VALIDATION_DISABLED) {
            long currentTimeInMicros = System.currentTimeMillis() * 1000;

            long driftInMicros = span.timestamp() > currentTimeInMicros
                    ? span.timestamp() - currentTimeInMicros
                    : currentTimeInMicros - span.timestamp();

            long driftInSeconds = driftInMicros / 1000 / 1000;

            if (driftInSeconds > maxTimestampDriftSeconds) {
                LOGGER.error(message -> message.operation("isSpanValid").msg("invalid timestamp")
                        .spanId(span::id).and("traceId", span::traceId).and("service", span::localServiceName)
                        .and("timestamp", span::timestamp).and("driftInSeconds", driftInSeconds));
                return false;
            }
        }

        return true;
    }
}
