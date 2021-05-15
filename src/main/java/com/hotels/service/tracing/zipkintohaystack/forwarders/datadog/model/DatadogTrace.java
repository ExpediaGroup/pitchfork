package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigInteger;
import java.util.Map;

public record DatadogTrace(long duration,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           Integer error,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           Map<String, String> meta,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           Map<String, Double> metrics,
                           String name,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           BigInteger parent_id,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           String resource,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           String service,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           BigInteger span_id,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           BigInteger start,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           BigInteger trace_id,
                           @JsonInclude(JsonInclude.Include.NON_NULL)
                           TypeEnum type) {
}
