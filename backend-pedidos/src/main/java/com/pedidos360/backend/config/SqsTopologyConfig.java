package com.pedidos360.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsTopologyConfig {

    private static final Logger log = LoggerFactory.getLogger(SqsTopologyConfig.class);

    @Value("${aws.region.static:us-east-1}")
    private String region;

    // Acepta los nombres del formato de AWS Academy (aws_access_key_id...) y los nombres con puntos
    @Value("${aws.credentials.access-key:${aws_access_key_id:}}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:${aws_secret_access_key:}}")
    private String secretKey;

    @Value("${aws.credentials.session-token:${aws_session_token:}}")
    private String sessionToken;

    private AwsCredentialsProvider proveedor;

    private synchronized AwsCredentialsProvider credenciales() {
        if (proveedor != null) {
            return proveedor;
        }
        String ak = accessKey == null ? "" : accessKey.trim();
        String sk = secretKey == null ? "" : secretKey.trim();
        String st = sessionToken == null ? "" : sessionToken.trim();

        if (ak.isEmpty() || sk.isEmpty()) {
            log.warn("Credenciales AWS: no hay claves en el proyecto, se usa la cadena por defecto (~/.aws/credentials o variables de entorno)");
            proveedor = DefaultCredentialsProvider.create();
        } else if (st.isEmpty()) {
            log.info("Credenciales AWS: tomadas del proyecto (sin session token)");
            proveedor = StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk));
        } else {
            log.info("Credenciales AWS: tomadas del proyecto (con session token)");
            proveedor = StaticCredentialsProvider.create(AwsSessionCredentials.create(ak, sk, st));
        }
        return proveedor;
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credenciales())
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credenciales())
                .build();
    }
}