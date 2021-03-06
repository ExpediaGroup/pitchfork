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
import com.expedia.pitchfork.systems.common.SpanValidator;
import com.expedia.pitchfork.systems.zipkin.ingress.kafka.properties.KafkaIngressConfigProperties;
import com.expedia.pitchfork.monitoring.metrics.MetersProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(KafkaIngressConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.ingress.kafka.enabled", havingValue = "true")
@Configuration
public class KafkaConsumerSpringConfig {

    @Bean(initMethod = "initialize", destroyMethod = "shutdown")
    public KafkaRecordsConsumer kafkaRecordsConsumer(Fork fork, SpanValidator validator,
                                                     KafkaIngressConfigProperties properties,
                                                     MetersProvider meters,
                                                     MeterRegistry meterRegistry) {
        return new KafkaRecordsConsumer(fork, validator, properties, meters, meterRegistry);
    }
}
