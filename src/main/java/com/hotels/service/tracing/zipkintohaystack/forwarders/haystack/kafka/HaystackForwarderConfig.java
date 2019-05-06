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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kafka.properties.KafkaForwarderConfigProperties;

@EnableConfigurationProperties(KafkaForwarderConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.forwarders.haystack.kafka.enabled", havingValue = "true")
@Configuration
public class HaystackForwarderConfig {

    @Bean
    public HaystackKafkaSpanForwarder haystackForwarder(KafkaForwarderConfigProperties properties) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.RETRIES_CONFIG, 2);
        props.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, SECONDS.toMillis(1));
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "haystack-proxy");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(props);

        return new HaystackKafkaSpanForwarder(kafkaProducer, properties.getTopic());
    }
}
