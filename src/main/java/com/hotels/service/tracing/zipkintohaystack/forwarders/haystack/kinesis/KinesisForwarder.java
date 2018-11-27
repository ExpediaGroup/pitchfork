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
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kinesis;

import static com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.HaystackDomainConverter.fromZipkinV2;

import java.nio.ByteBuffer;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.hotels.service.tracing.zipkintohaystack.LogFormatEnforcer;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import zipkin2.Span;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format,
 * converts to Haystack domain and pushes to a {@code Kinesis} stream.
 */
public class KinesisForwarder implements SpanForwarder {

    private static final LogFormatEnforcer LOGGER = LogFormatEnforcer.loggerFor(KinesisForwarder.class);

    private final AmazonKinesis producer;
    private final String streamName;

    public KinesisForwarder(AmazonKinesis producer, String streamName) {
        this.producer = producer;
        this.streamName = streamName;
    }

    @Override
    public void process(Span input) {
        LOGGER.debug(message -> message.operation("process").span(input));

        com.expedia.open.tracing.Span span = fromZipkinV2(input);

        byte[] value = span.toByteArray();

        // TODO: metrics with success/failures
        producer.putRecord(streamName, ByteBuffer.wrap(value), input.traceId());
    }
}
