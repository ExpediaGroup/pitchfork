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
package com.expedia.pitchfork.systems.zipkin.forwarder;

import com.expedia.pitchfork.systems.common.SpanForwarder;
import com.expedia.pitchfork.monitoring.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import okhttp3.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format re-encodes it in {@code Zipkin V2} format and pushes it to a {@code Zipkin} server.
 */
public class ZipkinForwarder implements SpanForwarder {
    private static final Logger logger = LoggerFactory.getLogger(ZipkinForwarder.class);

    private final Reporter<Span> spanReporter;

    public ZipkinForwarder(String endpoint,
                           int maxInFlightRequests,
                           int writeTimeoutMillis,
                           boolean compressionEnabled,
                           int maxIdleConnections,
                           boolean ignoreSslErrors,
                           int queuedMaxSpans,
                           Encoding encoding,
                           MetersProvider metersProvider) {
        try {
            OkHttpSender.Builder builder = OkHttpSender.newBuilder()
                    .endpoint(endpoint)
                    .maxRequests(maxInFlightRequests)
                    .writeTimeout(writeTimeoutMillis)
                    .encoding(encoding)
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
            var sender = builder.build();

            var successCounter = metersProvider.forwarderCounter("zipkin", true);
            var failureCounter = metersProvider.forwarderCounter("zipkin", false);

            this.spanReporter = setupReporter(sender, queuedMaxSpans, successCounter, failureCounter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(zipkin2.Span input) {
        logger.debug("operation=process, spanId={}", input);

        spanReporter.report(input);
    }

    private Reporter<Span> setupReporter(OkHttpSender sender, int queuedMaxSpans, Counter successCounter, Counter failureCounter) {
        return AsyncReporter.builder(sender)
                .metrics(new ZipkinForwarderMetrics(successCounter, failureCounter))
                .queuedMaxSpans(queuedMaxSpans)
                .build();
    }
}
