package com.pedidos360.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;

@Configuration
public class KinesisConfig {

    @Value("${aws.region.static:us-east-1}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key:${aws_access_key_id:}}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:${aws_secret_access_key:}}")
    private String secretKey;

    @Value("${spring.cloud.aws.credentials.session-token:${aws_session_token:}}")
    private String sessionToken;

    private AwsCredentialsProvider obtenerCredenciales() {
        String ak = accessKey == null ? "" : accessKey.trim();
        String sk = secretKey == null ? "" : secretKey.trim();
        String st = sessionToken == null ? "" : sessionToken.trim();

        if (ak.isEmpty() || sk.isEmpty()) {
            return DefaultCredentialsProvider.create();
        } else if (st.isEmpty()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk));
        } else {
            return StaticCredentialsProvider.create(AwsSessionCredentials.create(ak, sk, st));
        }
    }

    @Bean
    public KinesisClient kinesisClient() {
        return KinesisClient.builder()
                .region(Region.of(region))
                .credentialsProvider(obtenerCredenciales())
                .build();
    }

    @Bean
    public KinesisAsyncClient kinesisAsyncClient() {
        return KinesisAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(obtenerCredenciales())
                .build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(obtenerCredenciales())
                .build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(obtenerCredenciales())
                .build();
    }
}