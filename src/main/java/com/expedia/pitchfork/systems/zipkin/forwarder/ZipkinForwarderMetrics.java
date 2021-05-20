package com.expedia.pitchfork.systems.zipkin.forwarder;

import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.reporter.ReporterMetrics;

/**
 * Metrics for the Zipkin forwarder.
 */
public class ZipkinForwarderMetrics implements ReporterMetrics {

    private static final Logger logger = LoggerFactory.getLogger(ZipkinForwarderMetrics.class);

    private final Counter successCounter;
    private final Counter failureCounter;

    public ZipkinForwarderMetrics(Counter successCounter, Counter failureCounter) {
        this.successCounter = successCounter;
        this.failureCounter = failureCounter;
    }

    @Override
    public void incrementMessages() {
    }

    @Override
    public void incrementMessagesDropped(Throwable cause) {
        logger.debug("Messages dropped", cause);
        logger.error("Messages dropped with error {}", cause.getMessage());
    }

    @Override
    public void incrementSpans(int quantity) {
        successCounter.increment(quantity);
        logger.debug("Successfully wrote {} spans", quantity);
    }

    @Override
    public void incrementSpanBytes(int quantity) {
    }

    @Override
    public void incrementMessageBytes(int quantity) {
    }

    @Override
    public void incrementSpansDropped(int quantity) {
        failureCounter.increment(quantity);
    }

    @Override
    public void updateQueuedSpans(int update) {
    }

    @Override
    public void updateQueuedBytes(int update) {
    }
}
