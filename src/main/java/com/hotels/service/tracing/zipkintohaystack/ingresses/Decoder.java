package com.hotels.service.tracing.zipkintohaystack.ingresses;

import zipkin2.Span;

import java.util.Collection;

public interface Decoder {

    Collection<Span> decodeList(byte[] serialized);
}
