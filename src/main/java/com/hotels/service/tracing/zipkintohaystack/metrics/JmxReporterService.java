package com.hotels.service.tracing.zipkintohaystack.metrics;

import com.codahale.metrics.JmxReporter;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


@Component
public class JmxReporterService {

    @Inject
    public void JmxReporterService( JmxMeterRegistry jmxMeterRegistry ) {
        JmxReporter.forRegistry(jmxMeterRegistry.getDropwizardRegistry()).build().start();
    }
}
