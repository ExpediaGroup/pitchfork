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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.hotels.service.tracing.zipkintohaystack.ingresses.rabbitmq.properties.RabbitMqIngressConfigProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@EnableConfigurationProperties(RabbitMqIngressConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.ingress.rabbitmq.enabled", havingValue = "true")
@Configuration
public class RabbitMqIngressSpringConfig {

    @Bean(destroyMethod = "close")
    public Connection rabbitMqConnection(RabbitMqIngressConfigProperties properties) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(properties.getUser());
        factory.setPassword(properties.getPassword());
        factory.setVirtualHost(properties.getVirtualHost());
        factory.setHost(properties.getHost());
        factory.setPort(properties.getPort());

        return factory.newConnection();
    }

    @Bean(destroyMethod = "close")
    public Channel rabbitMqChannel(Connection rabbitMqConnection) throws Exception {
        return rabbitMqConnection.createChannel();
    }

    @Bean(initMethod = "initialize")
    public RabbitMqConsumer rabbitMqConsumer(Channel channel, Fork fork, SpanValidator spanValidator, RabbitMqIngressConfigProperties properties) {
        var sourceFormat = properties.getSourceFormat();
        var queueName = properties.getQueueName();

        return new RabbitMqConsumer(channel, fork, spanValidator, sourceFormat, queueName);
    }
}
