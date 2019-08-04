package com.hotels.service.tracing.zipkintohaystack.metrics;


import com.codahale.metrics.jmx.JmxReporter;


import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class JmxReporterService {


    private  JmxMeterRegistry jmxMeterRegistry;


    @Bean
    public MeterRegistry JmxReporterService( Clock clock) {
        this.jmxMeterRegistry =  new JmxMeterRegistry(JmxConfig.DEFAULT, clock);
        System.out.println("JmxReporterService: "+jmxMeterRegistry);
        JmxReporter.forRegistry(jmxMeterRegistry.getDropwizardRegistry()).build().start();
        return this.jmxMeterRegistry;
    }

}
