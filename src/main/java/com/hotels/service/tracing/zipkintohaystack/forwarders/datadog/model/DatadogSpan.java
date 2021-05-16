package com.hotels.service.tracing.zipkintohaystack.forwarders.datadog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;
import java.util.Map;

/**
 * Model for a {@code Datadog} span
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatadogSpan(Long duration,
                          Integer error,
                          Map<String, String> meta,
                          Map<String, Double> metrics,
                          String name,
                          @JsonProperty("parent_id")
                          BigInteger parentId,
                          String resource,
                          String service,
                          @JsonProperty("span_id")
                          BigInteger spanId,
                          Long start,
                          @JsonProperty("trace_id")
                          BigInteger traceId,
                          TypeEnum type) {
}
