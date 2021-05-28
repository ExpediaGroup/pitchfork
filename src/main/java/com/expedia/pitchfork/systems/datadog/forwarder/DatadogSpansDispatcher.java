package com.expedia.pitchfork.systems.datadog.forwarder;

import com.expedia.pitchfork.systems.datadog.DatadogDomainConverter;
import com.expedia.pitchfork.systems.datadog.model.DatadogSpan;
import com.expedia.pitchfork.systems.datadog.forwarder.properties.DatadogForwarderConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@EnableConfigurationProperties(DatadogForwarderConfigProperties.class)
@ConditionalOnProperty(name = "pitchfork.forwarders.datadog.enabled", havingValue = "true")
@Component
public class DatadogSpansDispatcher implements Runnable {

    private static final long FIVE_SECONDS = 5 * 1000; // in millis
    private static final int MINIMUM_PENDING_SPANS = 100;
    private final Logger logger = LoggerFactory.getLogger(DatadogForwarder.class);

    private final WebClient datadogClient;
    private final ObjectWriter mapper;
    private final ArrayBlockingQueue<Span> pending;

    public DatadogSpansDispatcher(WebClient datadogClient,
                                  ObjectMapper mapper,
                                  DatadogForwarderConfigProperties properties) {
        this.datadogClient = datadogClient;
        this.mapper = mapper.writer();
        this.pending = new ArrayBlockingQueue<>(properties.getQueuedMaxSpans());
    }

    @Override
    public void run() {
        List<Span> spans = new ArrayList<>();
        long lastFlush = currentTimeMillis();

        while (true) {
            try {
                Span take = pending.poll(1, TimeUnit.SECONDS);

                if (take != null) {
                    spans.add(take);
                }

                if (shouldFlushSpans(spans.size(), lastFlush)) {
                    flush(spans);

                    lastFlush = currentTimeMillis();
                }
            } catch (InterruptedException e) {
                logger.error("operation=run", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Flush data if there's already a big number of them pending or if they've been waiting for over x seconds.
     */
    private boolean shouldFlushSpans(int size, long lastFlush) {
        long currentTime = currentTimeMillis();
        boolean enoughPendingSpans = size > MINIMUM_PENDING_SPANS;
        boolean spansPendingForOverXSeconds = (lastFlush + FIVE_SECONDS < currentTime) && size > 0;

        return enoughPendingSpans || spansPendingForOverXSeconds;
    }

    private void flush(List<Span> spans) {
        List<DatadogSpan> datadogSpans = spans.stream()
                .map(DatadogDomainConverter::fromZipkinV2)
                .collect(toList());

        Optional<String> body = serialize(datadogSpans);

        body.ifPresent(it -> {
            datadogClient.put()
                    .uri("/v0.3/traces")
                    .body(fromValue(it))
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(); // FIXME: reactive
        });

        spans.clear();
    }

    private Optional<String> serialize(List<DatadogSpan> spans) {
        try {
            var body = mapper.writeValueAsString(List.of(spans));
            return Optional.ofNullable(body);
        } catch (JsonProcessingException e) {
            logger.error("operation=serialize", e);
            return Optional.empty();
        }
    }

    /**
     * Add spans to the backlog queue.
     */
    public void addSpan(Span span) {
        try {
            this.pending.add(span);
        } catch (Exception e) {
            logger.error("operation=addSpan", e);
        }
    }
}
