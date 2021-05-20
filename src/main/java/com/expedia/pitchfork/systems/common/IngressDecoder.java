package com.expedia.pitchfork.systems.common;

import zipkin2.Span;

import java.util.Collection;

/**
 * Contract for a decoder of bytes {@link Span}.
 */
public interface IngressDecoder {

    /**
     * Returns a collection of spans from their serialized form.
     */
    Collection<Span> decodeList(byte[] serialized);
}
