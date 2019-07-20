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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.ingresses.kafka.properties.KafkaIngressConfigProperties;
import io.micrometer.core.instrument.Counter;
import reactor.core.publisher.Mono;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

public class KafkaConsumerLoop implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator validator;
    private final KafkaIngressConfigProperties config;
    private KafkaConsumer<String, byte[]> kafkaConsumer;
    private SpanBytesDecoder decoder;
    private Counter spansCounter;
    private List<String> sourceTopics;
    private int pollDurationMs;

    public KafkaConsumerLoop(KafkaIngressConfigProperties config, Fork fork, SpanValidator validator, SpanBytesDecoder decoder, Counter spansCounter) {
        this.fork = fork;
        this.config = config;
        this.validator = validator;
        this.decoder = decoder;
        this.spansCounter = spansCounter;
    }

    public void initialize() {
        this.sourceTopics = config.getSourceTopics();
        this.pollDurationMs = config.getPollDurationMs();

        String kafkaBrokers = config.getBootstrapServers();
        int autoCommitIntervalMs = config.getAutoCommitIntervalMs();
        boolean enableAutoCommit = config.isEnableAutoCommit();
        int sessionTimeoutMs = config.getSessionTimeoutMs();
        String autoOffsetReset = config.getAutoOffsetReset();

        this.kafkaConsumer = kafkaConsumer(kafkaBrokers, autoCommitIntervalMs, enableAutoCommit, sessionTimeoutMs, autoOffsetReset);
    }

    private KafkaConsumer<String, byte[]> kafkaConsumer(String kafkaBrokers, int autoCommitIntervalMs, boolean enableAutoCommit, int sessionTimeoutMs,
            String autoOffsetReset) {
        return new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers,
                        ConsumerConfig.GROUP_ID_CONFIG, "pitchfork",
                        ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs,
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit,
                        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset
                ),
                new StringDeserializer(),
                new ByteArrayDeserializer()
        );
    }

    @Override
    public void run() {
        try {
            kafkaConsumer.subscribe(sourceTopics);

            while (true) {
                var records = kafkaConsumer.poll(ofMillis(pollDurationMs));

                if (!records.isEmpty()) {
                    StreamSupport.stream(records.spliterator(), false)
                            .flatMap((Function<ConsumerRecord<String, byte[]>, Stream<Span>>) record -> decoder.decodeList(record.value())
                                    .stream())
                            .filter(validator::isSpanValid)
                            .peek(span -> spansCounter.increment())
                            .forEach(span -> fork.processSpan(span)
                                    .doOnError(throwable -> logger.warn("operation=fetchRecordsFromKafka", throwable))
                                    .onErrorResume(e -> Mono.empty())
                                    .blockLast());
                    // TODO: consider replacing with a reactor KafkaReceiver
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
