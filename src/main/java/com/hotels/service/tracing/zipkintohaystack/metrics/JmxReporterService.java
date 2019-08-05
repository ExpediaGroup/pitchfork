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


import com.codahale.metrics.jmx.JmxReporter;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "management.metrics.export.jmx.enabled", havingValue = "false")
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
