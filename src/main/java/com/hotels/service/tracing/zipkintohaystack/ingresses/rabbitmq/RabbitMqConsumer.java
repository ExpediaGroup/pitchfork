/*
 * Copyright 2019 Expedia, Inc.
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
package com.hotels.service.tracing.zipkintohaystack.ingresses.rabbitmq;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

import java.io.IOException;
import java.util.List;

public class RabbitMqConsumer extends DefaultConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Fork fork;
    private final SpanValidator spanValidator;
    private final SpanBytesDecoder decoder;
    private final String queueName;
    private final boolean autoAck;

    public RabbitMqConsumer(Channel channel, Fork fork, SpanValidator spanValidator, String sourceFormat, String queueName, boolean autoAck) {
        super(channel);

        this.fork = fork;
        this.spanValidator = spanValidator;
        this.decoder = SpanBytesDecoder.valueOf(sourceFormat);
        this.queueName = queueName;
        this.autoAck = autoAck;
    }

    public void initialize() {
        try {
            this.getChannel().basicConsume(queueName, autoAck, "pitchfork", this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        List<Span> spans = decoder.decodeList(body);

        spans.stream()
                .filter(spanValidator::isSpanValid)
                .forEach(fork::processSpan);

        if (!autoAck) {
            long deliveryTag = envelope.getDeliveryTag();

            this.getChannel().basicAck(deliveryTag, false);
        }
    }
}
