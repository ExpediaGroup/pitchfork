package com.hotels.service.tracing.zipkintohaystack.ingresses.rabbitmq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotels.service.tracing.zipkintohaystack.forwarders.Fork;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@ConditionalOnProperty(name = "pitchfork.ingress.rabbitmq.enabled", havingValue = "true")
@Configuration
public class RabbitMqIngressSpringConfig {

    @Bean(destroyMethod = "close")
    public Connection rabbitMqConnection(RabbitMqIngressConfigProperties properties) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        // "guest"/"guest" by default, limited to localhost connections
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
