package com.hotels.service.tracing.zipkintohaystack;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import java.util.Map;

import static java.lang.Boolean.TRUE;

public class ForwarderPropertyListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private static final String FORWARDER_ENABLED_PATTERN = "^pitchfork\\.forwarders\\..+\\.enabled$";

    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        Map<String, Object> systemProperties = event.getEnvironment().getSystemProperties();
        if (systemProperties.entrySet().stream()
                .noneMatch(property -> property.getKey().matches(FORWARDER_ENABLED_PATTERN) && TRUE.toString().equals(property.getValue()))) {
            throw new IllegalStateException("No span forwarders configured. See README.md for a list of available span forwarders.");
        }
    }
}
