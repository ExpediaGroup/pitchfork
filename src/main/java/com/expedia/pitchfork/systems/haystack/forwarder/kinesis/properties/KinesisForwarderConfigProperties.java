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
package com.expedia.pitchfork.systems.haystack.forwarder.kinesis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("pitchfork.forwarders.haystack.kinesis")
public class KinesisForwarderConfigProperties {
    private String streamName;
    @NestedConfigurationProperty
    private ClientConfigProperties client;
    @NestedConfigurationProperty
    private AuthConfigProperties auth;

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public ClientConfigProperties getClient() {
        return client;
    }

    public void setClient(ClientConfigProperties client) {
        this.client = client;
    }

    public AuthConfigProperties getAuth() {
        return auth;
    }

    public void setAuth(AuthConfigProperties auth) {
        this.auth = auth;
    }
}
