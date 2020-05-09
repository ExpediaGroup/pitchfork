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
package com.hotels.service.tracing.zipkintohaystack.forwarders;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import zipkin2.Span;

@Component
public class Fork {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SpanForwarder[] spanForwarders;

    public Fork(@Autowired(required = false) SpanForwarder... spanForwarders) {
        this.spanForwarders = spanForwarders == null ? new SpanForwarder[0] : spanForwarders;
    }

    @PostConstruct
    public void init() {
        if (spanForwarders.length == 0) {
            logger.warn("No span forwarders configured. Pitchfork will not process any span.");
        }
    }

    public Flux<Object> processSpan(Span span) {
        return Flux.fromArray(spanForwarders)
                .flatMap(forwarder -> Mono.fromRunnable(() -> forwarder.process(span))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
