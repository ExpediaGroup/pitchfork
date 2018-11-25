package com.hotels.service.tracing.zipkintohaystack.forwarders.noop;

import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import zipkin2.Span;

@Conditional(NoForwardersCondition.class)
@Component("fallbackForwarder")
public class FallbackForwarder implements SpanForwarder {
    @Override
    public void process(Span span) {
        throw new IllegalStateException();
    }
}
