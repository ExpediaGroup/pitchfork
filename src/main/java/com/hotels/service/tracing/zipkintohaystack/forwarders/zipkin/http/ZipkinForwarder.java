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

import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import okhttp3.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Callback;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format re-encodes it in {@code Zipkin V2} format and pushes it to a {@code Zipkin} server.
 */
public class ZipkinForwarder implements SpanForwarder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipkinForwarder.class);

    private final OkHttpSender sender;
    private final Counter successCounter;
    private final Counter failureCounter;

    public ZipkinForwarder(String endpoint, int maxInFlightRequests, int writeTimeoutMillis, boolean compressionEnabled, int maxIdleConnections, boolean ignoreSslErrors, MetersProvider metersProvider) {
        try {
            OkHttpSender.Builder builder = OkHttpSender.newBuilder()
                    .endpoint(endpoint)
                    .maxRequests(maxInFlightRequests)
                    .writeTimeout(writeTimeoutMillis)
                    .compressionEnabled(compressionEnabled);

            builder.clientBuilder()
                    .connectionPool(new ConnectionPool(maxIdleConnections, 5, TimeUnit.MINUTES))
                    .pingInterval(60, TimeUnit.SECONDS);

            if (ignoreSslErrors) {
                X509TrustManager trustAllCertsTrustManager = new TrustAllCertsTrustManager();

                // Install all trusting trust manager
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{trustAllCertsTrustManager}, new java.security.SecureRandom());

                builder.clientBuilder()
                        .sslSocketFactory(sslContext.getSocketFactory(), trustAllCertsTrustManager)
                        .hostnameVerifier((hostname, session) -> true);
            }
            this.sender = builder.build();
            this.successCounter = metersProvider.forwarderCounter("zipkin", true);
            this.failureCounter = metersProvider.forwarderCounter("zipkin", false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(zipkin2.Span span) {
        try {
            LOGGER.debug("operation=process, spanId={}", span);
            byte[] bytes = SpanBytesEncoder.JSON_V2.encode(span);
            sender.sendSpans(singletonList(bytes)).enqueue(new ZipkinCallback(span));
        } catch (Exception e) {
            failureCounter.increment();
            LOGGER.error("Unable to serialise span with span id {}", span.id());
        }
    }

    static class ZipkinCallback implements Callback<Void> {
        final zipkin2.Span span;

        ZipkinCallback(zipkin2.Span span) {
            this.span = span;
        }

        @Override
        public void onSuccess(Void value) {
            successCounter.increment();
            LOGGER.info("Successfully wrote span {}", span.id());
        }

        @Override
        public void onError(Throwable t) {
            failureCounter.increment();
            LOGGER.error("Unable to write span {}", span.id(), t);
        }
    }
}
