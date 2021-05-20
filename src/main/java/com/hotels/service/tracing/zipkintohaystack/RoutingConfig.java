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
package com.hotels.service.tracing.zipkintohaystack;

import com.hotels.service.tracing.zipkintohaystack.ingresses.datadog.DatadogSpansMessagePackDecoder;
import com.hotels.service.tracing.zipkintohaystack.ingresses.datadog.DatadogSpansJsonDecoder;
import com.hotels.service.tracing.zipkintohaystack.metrics.MetersProvider;
import io.netty.handler.codec.http.HttpContentDecompressor;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static zipkin2.codec.SpanBytesDecoder.*;

@Configuration
public class RoutingConfig {

    private static final MediaType APPLICATION_THRIFT = MediaType.valueOf("application/x-thrift");
    private static final MediaType APPLICATION_PROTOBUF = MediaType.valueOf("application/x-protobuf");

    /**
     * This service does not support any of the read operations.
     * At this moment we support {@code POST}s for the v1 api encoded in Json or Thrift, or for the v2 api in Json.
     */
    @Bean
    public RouterFunction<ServerResponse> myRoutes(ZipkinController zipkinController,
                                                   MetersProvider metersProvider,
                                                   DatadogSpansMessagePackDecoder datadogSpansMessagePackDecoder,
                                                   DatadogSpansJsonDecoder datadogSpansJsonDecoder) {
        var counterJsonV1 = metersProvider.getSpansCounter("http", "jsonv1");
        var counterJsonV2 = metersProvider.getSpansCounter("http", "jsonv2");
        var counterThrift = metersProvider.getSpansCounter("http", "thrift");
        var counterProtobuf = metersProvider.getSpansCounter("http", "protobuf");
        var counterDatadog = metersProvider.getSpansCounter("http", "datadog");

        return nest(method(HttpMethod.POST),
                nest(contentType(APPLICATION_JSON),
                        route(path("/api/v1/spans"), request -> zipkinController.addSpans(request, JSON_V1::decodeList, counterJsonV1))
                                .andRoute(path("/api/v2/spans"), request -> zipkinController.addSpans(request, JSON_V2::decodeList, counterJsonV2)))
                        .andRoute(contentType(APPLICATION_THRIFT), request -> zipkinController.addSpans(request, THRIFT::decodeList, counterThrift))
                        .andRoute(contentType(APPLICATION_PROTOBUF), request -> zipkinController.addSpans(request, PROTO3::decodeList, counterProtobuf)))
                .andNest(method(HttpMethod.PUT),
                        nest(contentType(new MediaType("application", "msgpack")),
                                route(path("/v0.3/traces"), request -> zipkinController.addSpans(request, datadogSpansMessagePackDecoder, counterDatadog)))
                                .andNest(contentType(APPLICATION_JSON),
                                        route(path("/v0.3/traces"), request -> zipkinController.addSpans(request, datadogSpansJsonDecoder, counterDatadog)))
                )
                .andRoute(RequestPredicates.all(), zipkinController::unmatched);
    }

    /**
     * Since we're impersonating a {@code Zipkin} server we need to support the same set of features.
     * One of the features is request compression, which we handle here by adding a {@link HttpContentDecompressor} to the {@code Netty} pipeline.
     */
    @Bean
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();

        factory.addServerCustomizers(builder -> builder
                .tcpConfiguration(tcpServer -> {
                    return tcpServer.doOnConnection(connection -> connection.addHandler("decompressor", new HttpContentDecompressor()));
                }));

        return factory;
    }
}
