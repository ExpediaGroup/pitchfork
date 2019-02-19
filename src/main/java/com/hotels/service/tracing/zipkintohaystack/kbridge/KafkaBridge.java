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
package com.hotels.service.tracing.zipkintohaystack.kbridge;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hotels.service.tracing.zipkintohaystack.PitchForkConfig;
import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;

@ConditionalOnProperty(name = "pitchfork.ingress.kafka.enabled", havingValue = "true")
@Component
public class KafkaBridge {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaStreams stream;

    private final PitchForkConfig pitchForkConfig;
    private final Fork fork;
    private final SpanValidator spanValidator;

    @Inject
    public KafkaBridge(PitchForkConfig pitchForkConfig, Fork fork, SpanValidator spanValidator) {
        this.pitchForkConfig = pitchForkConfig;
        this.fork = fork;
        this.spanValidator = spanValidator;
    }

    @PostConstruct
    public void initialize() {
        Serde<List<zipkin2.Span>> serde = buildSerde(pitchForkConfig.getSourceFormat());

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(pitchForkConfig.getSourceTopics(), Consumed.with(Serdes.ByteArray(), serde))
                .flatMapValues((ValueMapper<List<zipkin2.Span>, Iterable<zipkin2.Span>>) value -> value)
                .filter((key, span) -> spanValidator.isSpanValid(span))
                .foreach((key, span) -> fork.processSpan(span).subscribe()); // TODO: error handling

        Properties properties = new Properties();
        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, pitchForkConfig.getBootstrapServers());
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "pitchfork");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());

        stream = new KafkaStreams(builder.build(), properties);
        stream.setUncaughtExceptionHandler((t, e) -> logger.error("Stream has been shut down!!", e));
        stream.start();
        logger.info("Stream processor for bridge has started");
    }

    @PreDestroy
    public void stop() {
        stream.close();
    }

    private Serde<List<zipkin2.Span>> buildSerde(String format) {
        String valueserde = (format != null) ? format : "";
        Serde<List<zipkin2.Span>> serde = ZipkinSerDes.JSON_V2.serde;
        if (valueserde.equalsIgnoreCase("json_v1")) {
            serde = ZipkinSerDes.JSON_V1.serde;
        } else if (valueserde.equalsIgnoreCase("thrift")) {
            serde = ZipkinSerDes.THRIFT.serde;
        } else if (valueserde.equalsIgnoreCase("proto3")) {
            serde = ZipkinSerDes.PROTO3.serde;
        }
        return serde;
    }
}
