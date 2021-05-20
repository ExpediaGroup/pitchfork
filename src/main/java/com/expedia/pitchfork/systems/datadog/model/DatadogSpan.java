package com.expedia.pitchfork.systems.datadog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.Map;

/**
 * Model for a {@code Datadog} span
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatadogSpan(@JsonProperty("trace_id")
                          BigInteger traceId,
                          @JsonProperty("span_id")
                          BigInteger spanId,
                          @JsonProperty("parent_id")
                          BigInteger parentId,
                          Long start,
                          Long duration,
                          Integer error,
                          Map<String, String> meta,
                          Map<String, Double> metrics,
                          String name,
                          String resource,
                          String service,
                          String type) {
}
