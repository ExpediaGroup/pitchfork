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
package com.expedia.pitchfork.systems.datadog;

import com.expedia.pitchfork.systems.datadog.model.DatadogSpan;
import zipkin2.Annotation;
import zipkin2.Endpoint;
import zipkin2.Span;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.springframework.util.StringUtils.hasText;

/**
 * Converter between {@code Zipkin} and {@code Datadog} domains.
 */
public class DatadogDomainConverter {

    private static final Integer ERROR = 1;

    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Datadog} format.
     */
    public static DatadogSpan fromZipkinV2(zipkin2.Span zipkin) {
        Integer error = isError(zipkin);
        BigInteger parentId = hexadecimalToDecimal(zipkin.parentId());
        var spanId = hexadecimalToDecimal(zipkin.id());
        var traceId = hexadecimalToDecimal(zipkin.traceId());

        return new DatadogSpan(
                traceId,
                spanId,
                parentId,
                toNanos(zipkin.timestamp()),
                toNanos(zipkin.duration()),
                error,
                tags(zipkin.tags(), zipkin.annotations(), zipkin.kind()),
                emptyMap(),
                valueOrDefault(zipkin.name(), "span"),
                valueOrDefault(zipkin.name(), "resource"), // TODO: maybe derive resource from tags? http.method + http.path?
                zipkin.localServiceName(),
                "web"
        );
    }

    private static Map<String, String> tags(Map<String, String> tags, List<Annotation> annotations, Span.Kind kind) {
        Map<String, String> collected = new HashMap<>();

        if (kind != null) {
            collected.put("span.kind", kind.name());
        }

        tags.forEach(collected::put);

        return collected;
    }

    private static String valueOrDefault(String input, String defaultValue) {
        return input != null ? input : defaultValue;
    }

    private static Long toNanos(Long microseconds) {
        return microseconds != null ? MICROSECONDS.toNanos(microseconds) : null;
    }

    private static Long toMicros(Long nanoseconds) {
        return nanoseconds != null ? NANOSECONDS.toMicros(nanoseconds) : null;
    }

    private static BigInteger hexadecimalToDecimal(String input) {
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

    private static Integer isError(Span zipkin) {
        return zipkin.tags().containsKey("error") ? ERROR : null;
    }

    private static boolean isError(DatadogSpan datadog) {
        return ERROR.equals(datadog.error());
    }

    public static Span toZipkin(DatadogSpan datadogSpan) {
        var builder = Span.newBuilder();

        if (hasText(datadogSpan.name())) {
            builder.localEndpoint(Endpoint.newBuilder()
                    .serviceName(datadogSpan.service())
                    .build());
        }

        builder.name(datadogSpan.name());

        if (isError(datadogSpan)) {
            builder.putTag("error", "error");
        }
        if (datadogSpan.meta() != null) {
            datadogSpan.meta().forEach(builder::putTag);
        }

        if (datadogSpan.type() != null) {
            builder.putTag("type", datadogSpan.type());
        }
        if (datadogSpan.traceId() != null) {
            builder.traceId(decimalToHexadecimal(datadogSpan.traceId()));
        }
        if (datadogSpan.spanId() != null) {
            builder.id(decimalToHexadecimal(datadogSpan.spanId()));
        }
        if (datadogSpan.parentId() != null) {
            builder.parentId(decimalToHexadecimal(datadogSpan.parentId()));
        }
        if (datadogSpan.start() != null) {
            builder.timestamp(toMicros(datadogSpan.start()));
        }
        if (datadogSpan.duration() != null) {
            builder.duration(toMicros(datadogSpan.duration()));
        }
        // TODO: annotations, resource_name, ...

        return builder.build();
    }

    /**
     * Zipkin trace ids are 64 or 128 bits represented as 16 or 32 hex characters with '0' left padding
     * We always use 64 bits (16 chars) to maintain compatibility with Datadog.
     */
    private static String decimalToHexadecimal(BigInteger id) {
        return String.format("%016x", id);
    }
}
