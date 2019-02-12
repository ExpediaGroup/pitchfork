package com.hotels.service.tracing.zipkintohaystack.kbridge;

import static com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.HaystackDomainConverter.fromZipkinV2;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.expedia.open.tracing.Span;
import com.hotels.service.tracing.zipkintohaystack.PitchForkConfig;

@ConditionalOnProperty(name = "pitchfork.ingress.kafka.enabled", havingValue = "true")
@Component
public class KafkaBridge {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaStreams stream;

    @Inject
    PitchForkConfig pitchForkConfig;

    @PostConstruct
    public void KafkaBridge() {
        Serde<List<zipkin2.Span>> serde = buildSerde(pitchForkConfig.getSourceFormat());
        Properties kafkaproperties = new Properties();
        kafkaproperties.put("bootstrap.servers", pitchForkConfig.getBootstrapServers());

        if (!kafkaproperties.containsKey(StreamsConfig.APPLICATION_ID_CONFIG)) {
            kafkaproperties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "pitchfork");
        }
        if (!kafkaproperties.containsKey(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG)) {
            kafkaproperties.setProperty(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                    LogAndContinueExceptionHandler.class.getName());
        }

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(pitchForkConfig.getSourceTopics(), Consumed.with(Serdes.ByteArray(), serde))
                //avoid null messages
                .filter((k, v) -> v != null)
                .flatMapValues((ValueMapper<List<zipkin2.Span>, Iterable<zipkin2.Span>>) value -> value)
                //no empty traceids
                .filter((k, v) -> v.traceId() != null && !v.traceId().isEmpty())
                .map((k, v) -> traceidKeyMapper.apply(k, v))
                .mapValues(value -> value.toByteArray())
                .to(pitchForkConfig.getHaystackTopic(), Produced.with(Serdes.String(), Serdes.ByteArray()));

        stream = new KafkaStreams(builder.build(), kafkaproperties);
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

    private KeyValueMapper<byte[], zipkin2.Span, KeyValue<String, Span>> traceidKeyMapper =
            (key, value) -> {
                //since we cannot trust the key to be the traceid, just map the key to the traceid
                //from the span
                Span hsSpan = fromZipkinV2(value);
                return new KeyValue<>(hsSpan.getTraceId(), hsSpan);
            };

}
