package com.hotels.service.tracing.zipkintohaystack;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("pitchfork.ingress.kafka")
@Configuration
public class PitchForkConfig {
    boolean enabled;
    List<String> sourceTopics;
    String bootstrapServers;
    String sourceFormat;
    String haystackTopic;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
