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
package com.hotels.service.tracing.zipkintohaystack.forwarders.zipkin.http.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pitchfork.forwarders.zipkin.http")
public class ZipkinForwarderConfigProperties {
    private String endpoint;
    private int maxInFlightRequests;
    private int writeTimeoutMillis;
    private boolean compressionEnabled;
    private int maxIdleConnections;
    private boolean ignoreSslErrors;
    private int queuedMaxSpans;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getMaxInFlightRequests() {
        return maxInFlightRequests;
    }

    public void setMaxInFlightRequests(int maxInFlightRequests) {
        this.maxInFlightRequests = maxInFlightRequests;
    }

    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public boolean isIgnoreSslErrors() {
        return ignoreSslErrors;
    }

    public void setIgnoreSslErrors(boolean ignoreSslErrors) {
        this.ignoreSslErrors = ignoreSslErrors;
    }

    public int getQueuedMaxSpans() {
        return queuedMaxSpans;
    }

    public void setQueuedMaxSpans(int queuedMaxSpans) {
        this.queuedMaxSpans = queuedMaxSpans;
    }
}
