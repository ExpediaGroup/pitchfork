package com.hotels.service.tracing.zipkintohaystack.forwarders.noop;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

import static java.lang.Boolean.TRUE;

public class NoForwardersCondition implements Condition {
    private static final String FORWARDER_ENABLED_PATTERN = "^pitchfork\\.forwarders\\..+\\.enabled$";

    @Override
    public boolean matches(@NonNull ConditionContext conditionContext, @NonNull AnnotatedTypeMetadata annotatedTypeMetadata) {
        return ((AbstractEnvironment) conditionContext.getEnvironment()).getSystemProperties().entrySet().stream()
                .noneMatch(property -> property.getKey().matches(FORWARDER_ENABLED_PATTERN) && TRUE.toString().equals(property.getValue()));
    }
}
