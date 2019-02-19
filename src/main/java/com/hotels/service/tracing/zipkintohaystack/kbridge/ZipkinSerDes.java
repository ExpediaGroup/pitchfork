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
        ZipkinKafkaAdapter adapter = new ZipkinKafkaAdapter(decoder, SpanBytesEncoder.JSON_V2); // TODO: we don't need to define the encoder as spans go through the normal forwarding process
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
