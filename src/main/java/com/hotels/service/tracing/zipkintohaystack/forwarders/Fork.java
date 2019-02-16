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

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import zipkin2.Span;

@Component
public class Fork {

    private final SpanForwarder[] spanForwarders;
    private final ExecutorService threadPool;

    public Fork(@Value ("${pitchfork.forwarders.threadpool.size}") int forwardersThreadpoolSize,
                              @Autowired(required = false) SpanForwarder... spanForwarders) {
        this.spanForwarders = spanForwarders == null ? new SpanForwarder[0] : spanForwarders;
        this.threadPool = Executors.newFixedThreadPool(forwardersThreadpoolSize);
    }

    @PostConstruct
    public void init() {
        if (spanForwarders.length == 0) {
            throw new IllegalStateException("No span forwarders configured. See README.md for a list of available forwarders.");
        }
    }

    public List<Future<Void>> processSpan(Span span) {
        return Arrays.stream(spanForwarders)
                .map(producer -> runAsync(() -> producer.process(span), threadPool))
                .collect(toList());
    }
}
