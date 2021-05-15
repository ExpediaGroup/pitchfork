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
package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import zipkin2.Span;

import java.util.List;

@ConditionalOnProperty(name = "pitchfork.forwarders.datadog.enabled", havingValue = "true")
@Component
public class DatadogForwarder implements SpanForwarder {

    private final Logger logger = LoggerFactory.getLogger(DatadogForwarder.class);
    private final WebClient datadogClient;
    private final ObjectWriter mapper;

    public DatadogForwarder(WebClient datadogClient, ObjectMapper mapper) {
        this.datadogClient = datadogClient;
        this.mapper = mapper.writer();
    }

    @Override
    public void process(Span span) {
        logger.info("operation=process, span={}", span);

        DatadogTrace abc = DatadogDomainConverter.fromZipkinV2(span);
        // list of spans

        String s = null;
        try {
            s = mapper.writeValueAsString(List.of(List.of(abc)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        var urlBuilder = UriComponentsBuilder
                .fromUriString("http://localhost:8126")
//                .fromUriString("http://www.example.com")
                .path("/v0.3/traces");



        datadogClient.put()
                .uri(urlBuilder.build().toUri())
                .body(BodyInserters.fromValue(s))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();


        System.out.println("sss");
    }
}
