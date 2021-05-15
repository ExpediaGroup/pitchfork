package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class HttpClientSpringConfig {

    @Bean("datadogClient")
    public WebClient createWebClient(WebClient.Builder webClientBuilder) {
        Integer connectTimeoutMillis = 10000;
        int readTimeoutMillis = 10000;
        HttpClient httpClient =
                HttpClient.create(ConnectionProvider.builder(ConnectionProvider.DEFAULT_POOL_LEASING_STRATEGY)
                        .maxConnections(10).build())
                        .tcpConfiguration(client ->
                                client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                                        .doOnConnected(conn -> conn
                                                .addHandlerLast(new ReadTimeoutHandler(readTimeoutMillis))));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        return webClientBuilder
                .clientConnector(connector)
                //.baseUrl("http://localhost:8126")
//                .baseUrl("http://www.example.com:80")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
