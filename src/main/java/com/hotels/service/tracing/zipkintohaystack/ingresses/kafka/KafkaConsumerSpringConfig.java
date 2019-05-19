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

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.ingresses.kafka.properties.KafkaIngressConfigProperties;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;

@EnableConfigurationProperties(KafkaIngressConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.ingress.kafka.enabled", havingValue = "true")
@Configuration
public class KafkaConsumerSpringConfig {

    @Bean
    public KafkaConsumer<String, byte[]> kafkaConsumer(KafkaIngressConfigProperties config) {
        String kafkaBrokers = config.getBootstrapServers();
        List<String> sourceTopics = config.getSourceTopics();
        int autoCommitIntervalMs = config.getAutoCommitIntervalMs();
        boolean enableAutoCommit = config.isEnableAutoCommit();
        int sessionTimeoutMs = config.getSessionTimeoutMs();
        String autoOffsetReset = config.getAutoOffsetReset();

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(
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

        consumer.subscribe(sourceTopics);

        return consumer;
    }

    @Bean(initMethod = "initialize")
    public KafkaRecordsConsumer kafkaRecordsConsumer(Fork fork, SpanValidator spanValidator, KafkaConsumer<String, byte[]> kafkaConsumer, KafkaIngressConfigProperties config, MetersProvider metersProvider) {
        return new KafkaRecordsConsumer(fork, spanValidator, kafkaConsumer, config, metersProvider);
    }
}
