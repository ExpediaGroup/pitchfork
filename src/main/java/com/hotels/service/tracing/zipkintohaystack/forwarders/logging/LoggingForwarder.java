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
package com.hotels.service.tracing.zipkintohaystack.forwarders.logging;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import zipkin2.Span;

@ConditionalOnProperty(name = "pitchfork.forwarders.logging.enabled", havingValue = "true")
@Component
public class LoggingForwarder implements SpanForwarder {

    private final Logger logger = LoggerFactory.getLogger(LoggingForwarder.class);
    private boolean logFullSpan;

    @Inject
    public LoggingForwarder(@Value("${pitchfork.forwarders.logging.log-full-span}") boolean logFullSpan) {
        this.logFullSpan = logFullSpan;
    }

    @Override
    public void process(Span span) {
        if (logFullSpan) {
            logger.info("operation=process, span={}", span);
        } else {
            logger.info("operation=process, spanId={}", span.id());
        }
    }
}
