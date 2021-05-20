package com.expedia.pitchfork.systems.datadog.forwarder.client;

import com.expedia.pitchfork.systems.datadog.forwarder.properties.DatadogForwarderConfigProperties;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static reactor.netty.resources.ConnectionProvider.DEFAULT_POOL_LEASING_STRATEGY;

@EnableConfigurationProperties(DatadogForwarderConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.forwarders.datadog.enabled", havingValue = "true")
@Configuration
public class HttpClientSpringConfig {

    @Bean("datadogClient")
    public WebClient createWebClient(WebClient.Builder webClientBuilder, DatadogForwarderConfigProperties properties) {
        HttpClient httpClient =
                HttpClient.create(ConnectionProvider.builder(DEFAULT_POOL_LEASING_STRATEGY)
                        .maxConnections(properties.getMaxConnections()).build())
                        .tcpConfiguration(client ->
                                client.option(CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                                        .doOnConnected(conn -> conn
                                                .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutMs()))));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return webClientBuilder
                .clientConnector(connector)
                .baseUrl("http://" + properties.getHost() + ":" + properties.getPort())
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
    }
}
