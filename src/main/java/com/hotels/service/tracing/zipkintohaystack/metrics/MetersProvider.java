/*
 * Copyright 2019 Expedia, Inc.
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
package com.hotels.service.tracing.zipkintohaystack.metrics;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MetersProvider {

    private final MeterRegistry meterRegistry;

    @Inject
    public MetersProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Counter getSpansCounter(String protocol, String transport) {
        return Counter.builder("counter.pitchfork.spans")
                .tags("transport", transport, "protocol", protocol)
                .register(meterRegistry);
    }

    public Counter getInvalidSpansCounter() {
        return Counter.builder("counter.pitchfork.spans.invalid")
                .tags("invalid", "true")
                .register(meterRegistry);
    }
}
