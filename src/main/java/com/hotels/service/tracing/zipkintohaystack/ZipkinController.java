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

import static org.springframework.web.reactive.function.server.ServerResponse.notFound;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.Collection;
import java.util.function.Function;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.SpanValidator;

@RestController
public class ZipkinController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SpanForwarder[] spanForwarders;
    private final SpanValidator spanValidator;

    public ZipkinController(@Autowired SpanValidator spanValidator, @Autowired(required = false) SpanForwarder... spanForwarders) {
        this.spanForwarders = spanForwarders == null ? new SpanForwarder[0] : spanForwarders;
        this.spanValidator = spanValidator;
    }

    @PostConstruct
    public void init() {
        if (spanForwarders.length == 0) {
            throw new IllegalStateException("No span forwarders configured. See README.md for a list of available forwarders.");
        }
    }

    /**
     * Unmatched requests made to this service will be logged by this function.
     */
    @NonNull
    public Mono<ServerResponse> unmatched(ServerRequest serverRequest) {
        return serverRequest
                .bodyToMono(String.class)
                .doOnError(throwable -> logger.warn("operation=unmatched", throwable))
                .doOnNext(body -> logger.info("operation=log, path={}, headers={}", serverRequest.path(), serverRequest.headers()))
                .then(notFound().build());
    }

    /**
     * Valid requests made to this service will be handled by this function.
     * It submits the reported spans to the registered {@link SpanForwarder} asynchronously and waits until they all complete.
     */
    public Mono<ServerResponse> addSpans(ServerRequest serverRequest, SpanBytesDecoder decoder) {
        return serverRequest
                .bodyToMono(byte[].class)
                .flatMapIterable(decodeList(decoder))
                .filter(spanValidator::isSpanValid)
                .flatMap(span -> Flux.fromArray(spanForwarders)
                    .flatMap(spanForwarder -> Mono.fromRunnable(() -> spanForwarder.process(span))
                        .subscribeOn(Schedulers.elastic())
                    )
                )
                .doOnError(throwable -> logger.warn("operation=addSpans", throwable))
                .then(ok().body(BodyInserters.empty()));
    }

    private Function<byte[], Iterable<Span>> decodeList(SpanBytesDecoder decoder) {
        return bytes -> (Collection<Span>) decoder.decodeList(bytes);
    }
}
