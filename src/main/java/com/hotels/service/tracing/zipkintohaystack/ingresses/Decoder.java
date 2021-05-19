package com.hotels.service.tracing.zipkintohaystack.ingresses;

import zipkin2.Span;

import java.util.Collection;

/**
 * Contract for a decoder of bytes {@link Span}.
 */
public interface Decoder {

    /**
     * Returns a collection of spans from their serialized form.
     */
    Collection<Span> decodeList(byte[] serialized);
}
