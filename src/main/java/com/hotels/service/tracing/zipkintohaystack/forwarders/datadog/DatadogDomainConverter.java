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
package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog;

import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogSpan;
import zipkin2.Annotation;
import zipkin2.Span;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Converter between {@code Zipkin} and {@code Datadog} domains.
 */
public class DatadogDomainConverter {

    public static final Long MILLIS_TO_NANOS = 1_000_000L;
    private static final Integer ERROR = 1;

    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Datadog} format.
     */
    public static DatadogSpan fromZipkinV2(zipkin2.Span zipkin) {
        Integer error = isError(zipkin);
        BigInteger parentId = convertId(zipkin.parentId());
        var spanId = convertId(zipkin.id());
        var traceId = convertId(zipkin.traceId());

        return new DatadogSpan(
                toNanos(zipkin.duration()),
                error,
                tags(zipkin.tags(), zipkin.annotations()), // TODO: add 'kind' to tag?
                emptyMap(),
                valueOrDefault(zipkin.name(), "span"),
                parentId,
                valueOrDefault(zipkin.name(), "resource"), // TODO: maybe derive resource from tags? http.method + http.path?
                zipkin.localServiceName(),
                spanId,
                toNanos(zipkin.timestamp()),
                traceId,
                null // TODO: TypeEnum.web
        );
    }

    private static Integer isError(Span zipkin) {
        return zipkin.tags().containsKey("error") ? ERROR : null;
    }

    private static Map<String, String> tags(Map<String, String> tags, List<Annotation> annotations) {
        // TODO: add annotations?
        return tags;
    }

    private static String valueOrDefault(String input, String defaultValue) {
        return input != null ? input : defaultValue;
    }

    private static Long toNanos(Long timestamp) {
        return timestamp != null ? timestamp * MILLIS_TO_NANOS : null;
    }

    private static BigInteger convertId(String input) {
        // B3 accepts ids with different sizes (64 or 128 bits).
        // To make this work we truncate larger than 64 bit ones (16 chars).
        if (input == null) {
            return null;
        } else if (input.length() > 16) {
            return new BigInteger(input.substring(16), 16);
        } else {
            return new BigInteger(input, 16);
        }
    }
}
