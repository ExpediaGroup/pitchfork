package com.hotels.service.tracing.zipkintohaystack.kbridge;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;


import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

public enum ZipkinSerDes{

    JSON_V1(SpanBytesDecoder.JSON_V1),
    JSON_V2(SpanBytesDecoder.JSON_V2),
    THRIFT(SpanBytesDecoder.THRIFT),
    PROTO3(SpanBytesDecoder.PROTO3);

    public Serde<Span> serde;

    ZipkinSerDes(SpanBytesDecoder decoder){
        ZipkinKafkaAdapter adapter = new ZipkinKafkaAdapter(decoder);
        serde = Serdes.serdeFrom(adapter, adapter);
    }

    private static class ZipkinKafkaAdapter implements Deserializer<Span>, Serializer<Span>{
        private SpanBytesDecoder decoder;

        public ZipkinKafkaAdapter(SpanBytesDecoder decoder){
            this.decoder = decoder;
        }


        @Override
        public void configure(Map<String, ?> map, boolean b) {
            //Noop
        }

        @Override
        public Span deserialize(String s, byte[] bytes) {
            return decoder.decodeOne(bytes);
        }

        @Override
        public byte[] serialize(String topic, Span span) {
            return span.toString().getBytes();
        }

        @Override
        public void close() {
            //Noop
        }
    }
}
