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
package com.expedia.pitchfork.systems.datadog.forwarder;

import com.expedia.pitchfork.systems.common.SpanForwarder;
import com.expedia.pitchfork.systems.datadog.forwarder.properties.DatadogForwarderConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import zipkin2.Span;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

/**
 * Forwarder of spans to an async {@code Datadog} collector.
 * Spans are sent to a dispatcher that will process them in the background.
 */
@EnableConfigurationProperties(DatadogForwarderConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.forwarders.datadog.enabled", havingValue = "true")
@Component
public class DatadogForwarder implements SpanForwarder {

    private final Logger logger = LoggerFactory.getLogger(DatadogForwarder.class);
    private final DatadogSpansDispatcher spansDispatcher;

    public DatadogForwarder(DatadogSpansDispatcher spansDispatcher) {
        this.spansDispatcher = spansDispatcher;
    }

    @PostConstruct
    public void initialize() {
        CompletableFuture.runAsync(spansDispatcher);
    }

    @Override
    public void process(Span span) {
        logger.debug("operation=process, span={}", span);

        spansDispatcher.addSpan(span);
    }
}
