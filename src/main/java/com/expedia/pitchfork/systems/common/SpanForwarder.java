/*
 * Copyright 2018 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 *
 */
package com.expedia.pitchfork.systems.common;

import zipkin2.Span;

/**
 * Contract for a forwarder of {@code Zipkin} {@link Span}s.
 * Incoming spans can be forwarded to multiple systems for example {@code Zipkin}, {@code Haystack}, or others. These forwarders must implement this interface.
 */
public interface SpanForwarder {

    /**
     * Accepts a {@code Zipkin} {@link Span} and does something with it.
     */
    void process(Span span);
}
