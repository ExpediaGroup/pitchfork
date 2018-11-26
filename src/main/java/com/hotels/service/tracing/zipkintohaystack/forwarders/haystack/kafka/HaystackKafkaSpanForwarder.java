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
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.expedia.open.tracing.Span;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.HaystackDomainConverter;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format, converts to Haystack domain and pushes a {@code Kafka} stream.
 */
public class HaystackKafkaSpanForwarder implements SpanForwarder, AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HaystackDomainConverter domainConverter;
    private final String topic;
    private final Producer<String, byte[]> producer;

    public HaystackKafkaSpanForwarder(HaystackDomainConverter domainConverter, Producer<String, byte[]> producer, String topic) {
        this.domainConverter = domainConverter;
        this.topic = topic;
        this.producer = producer;
    }

    @Override
    public void process(zipkin2.Span input) {
        logger.debug("operation=process, span={}", input);

        Span span = domainConverter.fromZipkinV2(input);
        byte[] value = span.toByteArray();

        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, span.getTraceId(), value);

        // FIXME send() should return a future but it's blocking when kafka servers are unavailable
        // TODO: metrics with success/failures
        producer.send(record);
    }

    @Override
    public void close() {
        producer.flush();
        producer.close();
    }
}
