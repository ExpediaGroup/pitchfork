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
package com.hotels.service.tracing.zipkintohaystack.ingresses.kafka;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.ingresses.kafka.properties.KafkaIngressConfigProperties;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.time.Duration.ofMillis;

public class KafkaConsumerLoop implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator validator;
    private final KafkaIngressConfigProperties properties;
    private final SpanBytesDecoder decoder;
    private final Counter spansCounter;
    private KafkaConsumer<String, byte[]> kafkaConsumer;
    private List<String> sourceTopics;
    private int pollDurationMs;

    public KafkaConsumerLoop(KafkaIngressConfigProperties properties,
                             Fork fork,
                             SpanValidator validator,
                             SpanBytesDecoder decoder,
                             Counter spansCounter) {
        this.fork = fork;
        this.properties = properties;
        this.validator = validator;
        this.decoder = decoder;
        this.spansCounter = spansCounter;
    }

    public void initialize() {
        this.sourceTopics = properties.getSourceTopics();
        this.pollDurationMs = properties.getPollDurationMs();

        String kafkaBrokers = properties.getBootstrapServers();

        this.kafkaConsumer = kafkaConsumer(kafkaBrokers, properties.getOverrides());
    }

    private KafkaConsumer<String, byte[]> kafkaConsumer(String kafkaBrokers, Map<String, String> propertiesOverrides) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pitchfork");

        props.putAll(propertiesOverrides);

        return new KafkaConsumer<>(props, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Override
    public void run() {
        try {
            kafkaConsumer.subscribe(sourceTopics);

            while (true) {
                var records = kafkaConsumer.poll(ofMillis(pollDurationMs));

                for (ConsumerRecord<String, byte[]> record : records) {
                    List<Span> spans = decoder.decodeList(record.value());

                    spans.stream()
                            .filter(validator::isSpanValid)
                            .peek(span -> spansCounter.increment())
                            .forEach(fork::processSpan);
                }
            }
        } catch (WakeupException exception) {
            // ignore for shutdown
        } finally {
            kafkaConsumer.close();
        }
    }

    public void shutdown() {
        kafkaConsumer.wakeup();
    }
}
