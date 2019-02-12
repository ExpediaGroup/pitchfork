package com.hotels.service.tracing.zipkintohaystack.kbridge;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

public enum ZipkinSerDes {

    JSON_V1(SpanBytesDecoder.JSON_V1),
    JSON_V2(SpanBytesDecoder.JSON_V2),
    THRIFT(SpanBytesDecoder.THRIFT),
    PROTO3(SpanBytesDecoder.PROTO3);

    public Serde<List<Span>> serde;

    ZipkinSerDes(SpanBytesDecoder decoder) {
        ZipkinKafkaAdapter adapter = new ZipkinKafkaAdapter(decoder, SpanBytesEncoder.JSON_V2);
        serde = Serdes.serdeFrom(adapter, adapter);
    }

    private static class ZipkinKafkaAdapter implements Deserializer<List<Span>>, Serializer<List<Span>> {
        private SpanBytesDecoder decoder;
        private SpanBytesEncoder encoder;

        public ZipkinKafkaAdapter(SpanBytesDecoder decoder, SpanBytesEncoder encoder) {
            this.decoder = decoder;
            this.encoder = encoder;
        }

        @Override
        public void configure(Map<String, ?> map, boolean b) {
            //Noop
        }

        @Override
        public List<Span> deserialize(String s, byte[] bytes) {
            return decoder.decodeList(bytes);
        }

        @Override
        public byte[] serialize(String topic, List<Span> span) {
            return encoder.encodeList(span);
        }

        @Override
        public void close() {
            //Noop
        }
    }
}
