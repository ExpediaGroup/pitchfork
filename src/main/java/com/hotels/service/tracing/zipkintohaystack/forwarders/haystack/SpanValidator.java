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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;

/**
 * Validator for spans.
 * Please note that some or all checks are optional, for example, a span with a null timestamp is still a valid span.
 * These validations should be used if there's a need to identify or block bad actors.
 */
@Component
public class SpanValidator {

    private static final int VALIDATION_DISABLED = -1;

    private final Logger logger = LoggerFactory.getLogger(SpanValidator.class);
    private final boolean acceptNullTimestamps;
    private final int maxTimestampDriftSeconds;
    private final MetersProvider metersProvider;
    private Counter invalidSpansCounter;

    public SpanValidator(@Value("${pitchfork.validators.accept-null-timestamps}") boolean acceptNullTimestamps,
                         @Value("${pitchfork.validators.max-timestamp-drift-seconds}") int maxTimestampDriftSeconds,
                         MetersProvider metersProvider) {
        this.acceptNullTimestamps = acceptNullTimestamps;
        this.maxTimestampDriftSeconds = maxTimestampDriftSeconds;
        this.metersProvider = metersProvider;
    }

    @PostConstruct
    public void initialize() {
        invalidSpansCounter = metersProvider.getInvalidSpansCounter();
    }

    public boolean isSpanValid(zipkin2.Span span) {
        if (span.traceId() == null) {
            invalidSpansCounter.increment();
            logger.error("operation=isSpanValid, error='null traceId', service={}, spanId={}",
                    span.localServiceName(),
                    span.id());

            return false;
        }

        if (span.timestamp() == null && !acceptNullTimestamps) {
            invalidSpansCounter.increment();
            logger.error("operation=isSpanValid, error='null timestamp', service={}, traceId={}, spanId={}",
                    span.localServiceName(),
                    span.traceId(),
                    span.id());

            return false;
        }

        if (span.timestamp() != null && maxTimestampDriftSeconds != VALIDATION_DISABLED) {
            long currentTimeInMicros = System.currentTimeMillis() * 1000;

            long driftInMicros = span.timestamp() > currentTimeInMicros
                    ? span.timestamp() - currentTimeInMicros
                    : currentTimeInMicros - span.timestamp();

            long driftInSeconds = driftInMicros / 1000 / 1000;

            if (driftInSeconds > maxTimestampDriftSeconds) {
                invalidSpansCounter.increment();
                logger.error("operation=isSpanValid, error='invalid timestamp', driftInSeconds={} timestamp={}, service={}, traceId={}, spanId={}",
                        driftInSeconds,
                        span.timestamp(),
                        span.localServiceName(),
                        span.traceId(),
                        span.id());

                return false;
            }
        }

        return true;
    }
}
