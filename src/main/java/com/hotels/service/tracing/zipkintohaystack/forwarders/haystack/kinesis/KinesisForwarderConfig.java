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
package com.hotels.service.tracing.zipkintohaystack.forwarders.haystack.kinesis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;

@ConditionalOnProperty(name = "pitchfork.forwarders.haystack.kinesis.enabled", havingValue = "true")
@Configuration
public class KinesisForwarderConfig {

    @Bean
    public KinesisForwarder createProducer(@Value("${pitchfork.forwarders.haystack.kinesis.region-name}") String regionName,
            @Value("${pitchfork.forwarders.haystack.kinesis.signing-region-name}") String signingRegionName,
            @Value("${pitchfork.forwarders.haystack.kinesis.stream-name}") String streamName,
            @Value("${pitchfork.forwarders.haystack.kinesis.service-endpoint}") String serviceEndpoint,
            @Value("${pitchfork.forwarders.haystack.kinesis.authentication-type}") AwsAuthenticationTypeEnum authenticationType,
            @Value("${pitchfork.forwarders.haystack.kinesis.endpoint-config-type}") KinesisEndpointConfigurationEnum endpointConfiguration,
            @Value("${pitchfork.forwarders.haystack.kinesis.aws-access-key}") String awsAccessKey,
            @Value("${pitchfork.forwarders.haystack.kinesis.aws-secret-key}") String awsSecretKey) {
        var amazonKinesis = getProducerConfiguration(regionName, endpointConfiguration, authenticationType, awsAccessKey, awsSecretKey,
                serviceEndpoint, signingRegionName);

        return new KinesisForwarder(amazonKinesis, streamName);
    }

    private AmazonKinesis getProducerConfiguration(String regionName,
            KinesisEndpointConfigurationEnum endpointConfiguration,
            AwsAuthenticationTypeEnum authenticationType,
            String awsAccessKey,
            String awsSecretKey,
            String serviceEndpoint,
            String signingRegionName) {
        AWSCredentialsProvider credsProvider = null;

        // TODO: check required args are not null for these switches
        // TODO: dont force optional fields to be set (app should not fail because region-name is missing when endpoint config is set to CONFIGURATION)

        switch (authenticationType) {
        case DEFAULT:
            credsProvider = DefaultAWSCredentialsProviderChain.getInstance();
            break;
        case BASIC:
            credsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
            break;
        }

        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard()
                .withCredentials(credsProvider);

        switch (endpointConfiguration) {
        case CONFIGURATION:
            clientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegionName));
            break;
        case REGION:
            clientBuilder.withRegion(regionName);
            break;
        }

        return clientBuilder.build();
    }
}
