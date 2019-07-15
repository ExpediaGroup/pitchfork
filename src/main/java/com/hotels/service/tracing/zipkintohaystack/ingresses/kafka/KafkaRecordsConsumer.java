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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.ingresses.kafka.properties.KafkaIngressConfigProperties;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import zipkin2.codec.SpanBytesDecoder;

public class KafkaRecordsConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator spanValidator;
    private final KafkaIngressConfigProperties config;
    private final MetersProvider metersProvider;
    private final List<KafkaConsumerLoop> consumers = new ArrayList<>();

    public KafkaRecordsConsumer(Fork fork, SpanValidator spanValidator, KafkaIngressConfigProperties config, MetersProvider metersProvider) {
        this.fork = fork;
        this.spanValidator = spanValidator;
        this.metersProvider = metersProvider;
        this.config = config;
    }

    public void initialize() {
        logger.info("operation=initialize");

        String sourceFormat = config.getSourceFormat();
        SpanBytesDecoder decoder = SpanBytesDecoder.valueOf(sourceFormat);
        Counter spansCounter = metersProvider.getSpansCounter("tcp", "kafka");

        int numberOfConsumers = config.getNumberConsumers();

        ExecutorService executor = Executors.newFixedThreadPool(numberOfConsumers);

        for (int i = 0; i < numberOfConsumers; i++) {
            KafkaConsumerLoop consumer = new KafkaConsumerLoop(
                    config,
                    fork,
                    spanValidator,
                    decoder,
                    spansCounter);

            consumer.initialize();
            consumers.add(consumer);
            executor.submit(consumer);
        }
    }

    private void shutdown() {
        for (KafkaConsumerLoop consumer : consumers) {
            consumer.shutdown();
        }
    }
}
