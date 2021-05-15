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

import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogTrace;
import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.TypeEnum;

import java.math.BigInteger;
import java.util.Map;

/**
 * Converter between {@code Zipkin} and {@code Datadog} domains.
 */
public class DatadogDomainConverter {

    /**
     * Accepts a span in {@code Zipkin V2} format and returns a span in {@code Haystack} format.
     */
    public static DatadogTrace fromZipkinV2(zipkin2.Span zipkin) {
        Integer error = null;
        Map<String, String> meta = null;
        Map<String, Double> metrics = null;
        BigInteger parentId = null;//convertId(zipkin.parentId());
        var spanId = convertId(zipkin.id());
        var traceId = convertId(zipkin.traceId());

        DatadogTrace trace = new DatadogTrace(
                zipkin.duration(),
                error,
                meta,
                metrics,
                valueOrDefault(zipkin.name(), "span"),
                parentId,
                valueOrDefault(zipkin.name(), "span"),
                zipkin.localServiceName(),
                spanId,
                toNanos(zipkin.timestamp()),
                traceId,
                null//TypeEnum.custom
        );
        return trace;
    }

    private static String valueOrDefault(String input, String defaultValue) {
        return input != null ? input : defaultValue;
    }

    private static BigInteger toNanos(Long timestamp) {
        return BigInteger.valueOf(timestamp).multiply(BigInteger.valueOf(1000000));
    }

    private static BigInteger convertId(String input) {
        // B3 accepts ids with different sizes (64 or 128 bits).
        // To keep it simple we just assume they are 64. If they were 128 we would not be able to map them to Datadog
        // ids anyway.
        if (input != null) {
            return new BigInteger(input, 16);
        } else {
            return null;
        }
    }
}
