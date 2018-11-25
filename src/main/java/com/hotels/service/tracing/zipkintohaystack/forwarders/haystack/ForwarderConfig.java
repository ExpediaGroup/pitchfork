package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack;

import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ForwarderConfig {
    @Bean
    @ConditionalOnMissingBean(SpanForwarder.class)
    public SpanForwarder noopForwarder() {
        return span -> {
            throw new IllegalStateException();
        };
    }
}
