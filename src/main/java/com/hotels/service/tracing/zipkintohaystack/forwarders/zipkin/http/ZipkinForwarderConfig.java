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
package com.hotels.service.tracing.zipkintohaystack.forwarders.zipkin.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "pitchfork.forwarders.zipkin.http.enabled", havingValue = "true")
@Configuration
public class ZipkinForwarderConfig {

    @Bean
    public ZipkinForwarder createZipkinProducer(@Value("${pitchfork.forwarders.zipkin.http.endpoint}") String endpoint,
                                          @Value("${pitchfork.forwarders.zipkin.http.max-inflight-requests}") int maxInFlightRequests,
                                          @Value("${pitchfork.forwarders.zipkin.http.write-timeout-millis}") int writeTimeoutMillis,
                                          @Value("${pitchfork.forwarders.zipkin.http.compression-enabled}") boolean compressionEnabled) {
        return new ZipkinForwarder(endpoint, maxInFlightRequests, writeTimeoutMillis, compressionEnabled);
    }
}
