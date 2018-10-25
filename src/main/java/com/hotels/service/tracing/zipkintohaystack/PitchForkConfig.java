package com.hotels.service.tracing.zipkintohaystack;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@ConfigurationProperties("pitchfork")
@Configuration
@EnableConfigurationProperties
public class PitchForkConfig {
    Map<String, String> kafka;
    KafkaBridgeConf kakfaBridge;

    public void setKafka(Map<String, String> properties){
        this.kafka = properties;
    }

    public Map<String, String> getKafka(){
        return kafka;
    }

    public KafkaBridgeConf getKakfaBridge(){
        return kakfaBridge;
    }

    public void setKakfaBridge(KafkaBridgeConf conf){
        kakfaBridge = conf;
    }

    public static class KafkaBridgeConf{
        boolean enabled;
        List<String> sourceTopics;

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

        String sourceFormat;
        String haystackTopic;

    }
}
