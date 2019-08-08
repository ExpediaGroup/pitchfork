package com.hotels.service.tracing.zipkintohaystack.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customization of meter registries.
 */
@Configuration
public class MetricsSpringConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsSpringConfig.class);

    @ConditionalOnProperty(prefix = "management.metrics.export.graphite", name = "enabled", havingValue = "true")
    @Bean
    public GraphiteMeterRegistry graphiteMeterRegistry(@Value("${spring.application.name}") String name, GraphiteConfig graphiteConfig, Clock clock) {
        LOGGER.info("operation=graphiteMeterRegistry");

        return new GraphiteMeterRegistry(
                graphiteConfig,
                clock,
                (id, convention) -> name + "." + HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, NamingConvention.dot));
    }
}
