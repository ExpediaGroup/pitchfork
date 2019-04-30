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
