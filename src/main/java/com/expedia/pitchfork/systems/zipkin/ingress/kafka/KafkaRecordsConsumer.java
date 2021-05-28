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
package com.expedia.pitchfork.systems.zipkin.ingress.kafka;

import com.expedia.pitchfork.systems.common.Fork;
import com.expedia.pitchfork.systems.haystack.SpanValidator;
import com.expedia.pitchfork.systems.zipkin.ingress.kafka.properties.KafkaIngressConfigProperties;
import com.expedia.pitchfork.monitoring.metrics.MetersProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.codec.SpanBytesDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaRecordsConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator spanValidator;
    private final KafkaIngressConfigProperties properties;
    private final MetersProvider metersProvider;
    private final List<KafkaConsumerLoop> consumers = new ArrayList<>();
    private final MeterRegistry meterRegistry;

    public KafkaRecordsConsumer(Fork fork,
                                SpanValidator spanValidator,
                                KafkaIngressConfigProperties properties,
                                MetersProvider metersProvider,
                                MeterRegistry meterRegistry) {
        this.fork = fork;
        this.spanValidator = spanValidator;
        this.metersProvider = metersProvider;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public void initialize() {
        logger.info("operation=initialize");

        String sourceFormat = properties.getSourceFormat();
        SpanBytesDecoder decoder = SpanBytesDecoder.valueOf(sourceFormat);
        Counter spansCounter = metersProvider.getSpansCounter("tcp", "kafka");

        int numberOfConsumers = properties.getNumberConsumers();

        ExecutorService executor = Executors.newFixedThreadPool(numberOfConsumers);

        for (int i = 0; i < numberOfConsumers; i++) {
            KafkaConsumerLoop consumer = new KafkaConsumerLoop(
                    properties,
                    fork,
                    spanValidator,
                    decoder,
                    spansCounter,
                    meterRegistry);

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
