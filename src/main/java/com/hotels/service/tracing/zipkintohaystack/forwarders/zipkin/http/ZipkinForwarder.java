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
package com.hotels.service.tracing.zipkintohaystack.forwarders.zipkin.http;

import static java.util.Collections.singletonList;

import java.util.concurrent.TimeUnit;

import com.hotels.service.tracing.zipkintohaystack.LogFormatEnforcer;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import okhttp3.ConnectionPool;
import zipkin2.Callback;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format re-encodes it in {@code Zipkin V2} format and pushes it to a {@code Zipkin} server.
 */
public class ZipkinForwarder implements SpanForwarder {
    private static final LogFormatEnforcer LOGGER = LogFormatEnforcer.loggerFor(ZipkinForwarder.class);

    private final OkHttpSender sender;

    public ZipkinForwarder(String endpoint, int maxInFlightRequests, int writeTimeoutMillis, boolean compressionEnabled) {
        OkHttpSender.Builder builder = OkHttpSender.newBuilder()
                .endpoint(endpoint)
                .maxRequests(maxInFlightRequests)
                .writeTimeout(writeTimeoutMillis)
                .compressionEnabled(compressionEnabled);

        // TODO: make configurable
        int maxIdle = 50;

        builder.clientBuilder()
                .connectionPool(new ConnectionPool(maxIdle, 5, TimeUnit.MINUTES))
                .pingInterval(60, TimeUnit.SECONDS);

        this.sender = builder.build();
    }

    @Override
    public void process(zipkin2.Span span) {
        try {
            LOGGER.debug(message -> message.operation("process").span(span));
            byte[] bytes = SpanBytesEncoder.JSON_V2.encode(span);
            sender.sendSpans(singletonList(bytes)).enqueue(new ZipkinCallback(span));
        } catch (Exception e) {
            LOGGER.error(message -> message.operation("process").msg("Unable to serialise span").spanId(span::id));
        }
    }

    class ZipkinCallback implements Callback<Void> {
        zipkin2.Span span;

        ZipkinCallback(zipkin2.Span span) {
            this.span = span;
        }

        @Override
        public void onSuccess(Void value) {
            LOGGER.debug(message -> message.operation("onSuccess").msg("Successfully wrote span").spanId(span::id));
        }

        @Override
        public void onError(Throwable t) {
            LOGGER.error(message -> message.operation("onSuccess").msg("Unable to write span").spanId(span::id).exception(t));
        }
    }
}
