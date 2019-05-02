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

import static java.time.Duration.ofMillis;

import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import reactor.core.publisher.Mono;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

public class KafkaRecordsConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator spanValidator;
    private final KafkaConsumer<String, byte[]> kafkaConsumer;
    private final KafkaIngressConfigProperties config;
    private final MetersProvider metersProvider;
    private SpanBytesDecoder decoder;
    private Counter spansCounter;

    public KafkaRecordsConsumer(Fork fork, SpanValidator spanValidator, KafkaConsumer<String, byte[]> kafkaConsumer, KafkaIngressConfigProperties config, MetersProvider metersProvider) {
        this.fork = fork;
        this.spanValidator = spanValidator;
        this.kafkaConsumer = kafkaConsumer;
        this.metersProvider = metersProvider;
        this.config = config;
    }

    public void initialize() {
        String sourceFormat = config.getSourceFormat();
        decoder = SpanBytesDecoder.valueOf(sourceFormat);
        spansCounter = metersProvider.getSpansCounter("tcp", "kafka");

        Thread thread = new Thread(this::fetchRecordsFromKafka);
        thread.setDaemon(true);
        thread.start();
    }

    private void fetchRecordsFromKafka() {
        try {
            while (true) {
                var records = kafkaConsumer.poll(ofMillis(config.getPollDurationMs()));

                if (!records.isEmpty()) {
                    StreamSupport.stream(records.spliterator(), false)
                            .flatMap((Function<ConsumerRecord<String, byte[]>, Stream<Span>>) record -> decoder.decodeList(record.value()).stream())
                            .filter(spanValidator::isSpanValid)
                            .peek(span -> spansCounter.increment())
                            .forEach(span -> fork.processSpan(span)
                                    .doOnError(throwable -> logger.warn("operation=fetchRecordsFromKafka", throwable))
                                    .onErrorResume(e -> Mono.empty())
                                    .blockLast());
                    // TODO: consider replacing with a reactor KafkaReceiver
                }
            }
        } finally {
            kafkaConsumer.close();
        }
    }
}
