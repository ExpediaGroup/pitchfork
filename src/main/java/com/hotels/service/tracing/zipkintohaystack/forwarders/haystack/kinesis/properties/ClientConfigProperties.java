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
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kinesis.properties;

public class ClientConfigProperties {
    private ClientConfigurationEnum configType;
    private ClientRegionConfigProperties region;
    private ClientEndpointConfigProperties endpoint;

    public ClientConfigurationEnum getConfigType() {
        return configType;
    }

    public void setConfigType(ClientConfigurationEnum configType) {
        this.configType = configType;
    }

    public ClientRegionConfigProperties getRegion() {
        return region;
    }

    public void setRegion(ClientRegionConfigProperties region) {
        this.region = region;
    }

    public ClientEndpointConfigProperties getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(ClientEndpointConfigProperties endpoint) {
        this.endpoint = endpoint;
    }
}
