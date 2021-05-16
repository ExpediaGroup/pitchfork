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
import com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model.DatadogSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.Span;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

/**
 * Forwarder of spans to an HTTP {@code Datadog} collector.
 */
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

        DatadogSpan datadogSpan = DatadogDomainConverter.fromZipkinV2(span);
        Optional<String> body = getBody(datadogSpan);

        body.ifPresent(it -> {
            datadogClient.put()
                    .uri("/v0.3/traces")
                    .body(fromValue(it))
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(); // FIXME: reactive
        });
    }

    private Optional<String> getBody(DatadogSpan datadogSpan) {
        try {
            var body = mapper.writeValueAsString(List.of(List.of(datadogSpan)));
            return Optional.ofNullable(body);
        } catch (JsonProcessingException e) {
            logger.error("operation=getBody", e);
            return Optional.empty();
        }
    }
}
