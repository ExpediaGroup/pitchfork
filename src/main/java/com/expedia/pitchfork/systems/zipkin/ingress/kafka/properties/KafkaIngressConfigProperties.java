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
package com.expedia.pitchfork.systems.zipkin.ingress.kafka.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

@ConfigurationProperties("pitchfork.ingress.kafka")
public class KafkaIngressConfigProperties {
    private boolean enabled;
    private List<String> sourceTopics;
    private String bootstrapServers;
    private String sourceFormat;
    private String haystackTopic;
    private int pollDurationMs;
    private int numberConsumers;
    private Map<String, String> overrides = emptyMap();

    public Map<String, String> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, String> overrides) {
        this.overrides = overrides;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setNumberConsumers(int numberConsumers) {
        this.numberConsumers = numberConsumers;
    }

    public int getNumberConsumers() {
        return numberConsumers;
    }

    public List<String> getSourceTopics() {
        return sourceTopics;
    }

    public void setSourceTopics(List<String> sourceTopics) {
        this.sourceTopics = sourceTopics;
    }

    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getHaystackTopic() {
        return haystackTopic;
    }

    public void setHaystackTopic(String haystackTopic) {
        this.haystackTopic = haystackTopic;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public int getPollDurationMs() {
        return pollDurationMs;
    }

    public void setPollDurationMs(int pollDurationMs) {
        this.pollDurationMs = pollDurationMs;
    }
}
