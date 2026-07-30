package com.secondzip.backend.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectStorageConfig {

    @Value("${NCLOUD_ACCESS_KEY}")
    private String accessKey;

    @Value("${NCLOUD_SECRET_KEY}")
    private String secretKey;

    @Value("${NCLOUD_OBJECT_STORAGE_ENDPOINT}")
    private String endpoint;

    @Value("${NCLOUD_OBJECT_STORAGE_REGION}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials credentials =
                new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                endpoint,
                                region
                        )
                )
                .withCredentials(
                        new AWSStaticCredentialsProvider(credentials)
                )
                .withPathStyleAccessEnabled(true)
                .build();
    }
}