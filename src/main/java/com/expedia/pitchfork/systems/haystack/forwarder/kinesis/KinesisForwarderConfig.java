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
package com.expedia.pitchfork.systems.haystack.forwarder.kinesis;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.expedia.pitchfork.systems.haystack.forwarder.kinesis.properties.AuthConfigProperties;
import com.expedia.pitchfork.systems.haystack.forwarder.kinesis.properties.ClientConfigProperties;
import com.expedia.pitchfork.systems.haystack.forwarder.kinesis.properties.KinesisForwarderConfigProperties;
import com.expedia.pitchfork.monitoring.metrics.MetersProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({KinesisForwarderConfigProperties.class})
@ConditionalOnProperty(name = "pitchfork.forwarders.haystack.kinesis.enabled", havingValue = "true")
@Configuration
public class KinesisForwarderConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public KinesisForwarder createKinesisProducer(KinesisForwarderConfigProperties properties, MetersProvider metersProvider) {
        var amazonKinesis = getProducerConfiguration(
                properties.getClient(),
                properties.getAuth());

        return new KinesisForwarder(amazonKinesis, properties.getStreamName(), metersProvider);
    }

    private AmazonKinesis getProducerConfiguration(ClientConfigProperties client, AuthConfigProperties auth) {
        AWSCredentialsProvider credsProvider = null;

        var authenticationType = auth.getConfigType();

        switch (authenticationType) {
            case DEFAULT -> {
                logger.info("Configuring Kinesis auth with default credentials provider");
                credsProvider = DefaultAWSCredentialsProviderChain.getInstance();
            }
            case BASIC -> {
                logger.info("Configuring Kinesis auth with basic credentials");
                var awsAccessKey = auth.getBasic().getAwsAccessKey();
                var awsSecretKey = auth.getBasic().getAwsSecretKey();
                credsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
            }
        }

        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard()
                .withCredentials(credsProvider);

        var clientConfiguration = client.getConfigType();

        switch (clientConfiguration) {
            case ENDPOINT -> {
                var serviceEndpoint = client.getEndpoint().getServiceEndpoint();
                var signingRegionName = client.getEndpoint().getSigningRegionName();
                logger.info("Configuring Kinesis client with endpoint config. serviceEndpoint={}, signingRegionName={}",
                        serviceEndpoint,
                        signingRegionName);
                var endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegionName);
                clientBuilder.withEndpointConfiguration(endpointConfiguration);
            }
            case REGION -> {
                var regionName = client.getRegion().getRegionName();
                logger.info("Configuring Kinesis client with region config. regionName={}", regionName);
                clientBuilder.withRegion(regionName);
            }
        }

        return clientBuilder.build();
    }
}
