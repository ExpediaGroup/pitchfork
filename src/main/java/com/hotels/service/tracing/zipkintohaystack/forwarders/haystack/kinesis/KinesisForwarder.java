package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kinesis;

import static com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.HaystackDomainConverter.fromZipkinV2;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.hotels.service.tracing.zipkintohaystack.forwarders.SpanForwarder;
import zipkin2.Span;

/**
 * Implementation of a {@link SpanForwarder} that accepts a span in {@code Zipkin} format,
 * converts to Haystack domain and pushes to a {@code Kinesis} stream.
 */
public class KinesisForwarder implements SpanForwarder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AmazonKinesis producer;
    private final String streamName;

    public KinesisForwarder(AmazonKinesis producer, String streamName) {
        this.producer = producer;
        this.streamName = streamName;
    }

    @Override
    public void process(Span input) {
        logger.debug("operation=process, span={}", input);

        com.expedia.open.tracing.Span span = fromZipkinV2(input);
        byte[] value = span.toByteArray();

        // TODO: metrics with success/failures
        producer.putRecord(streamName, ByteBuffer.wrap(value), input.traceId());
    }
}
