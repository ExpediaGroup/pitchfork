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

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.expedia.open.tracing.Span;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.HaystackDomainConverter.fromZipkinV2;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format,
 * converts to Haystack domain and pushes to a {@code Kinesis} stream.
 */
public class KinesisForwarder implements SpanForwarder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AmazonKinesis producer;
    private final String streamName;
    private final Counter successCounter;
    private final Counter failureCounter;

    public KinesisForwarder(AmazonKinesis producer, String streamName, MetersProvider metersProvider) {
        this.producer = producer;
        this.streamName = streamName;
        this.successCounter = metersProvider.forwarderCounter("kinesis", true);
        this.failureCounter = metersProvider.forwarderCounter("kinesis", false);
    }

    @Override
    public void process(zipkin2.Span input) {
        logger.debug("operation=process, span={}", input);

        Span span = fromZipkinV2(input);

        byte[] value = span.toByteArray();

        try {
            producer.putRecord(streamName, ByteBuffer.wrap(value), span.getTraceId());
            successCounter.increment();
        } catch (Exception e) {
            failureCounter.increment();
            logger.error("Failed to send span to kinesis {}", input.id(), e);
        }
    }
}
