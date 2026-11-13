package com.pedidos360.microservicio_admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.ScalingType;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KinesisAdminService {

    private final KinesisClient kinesisClient;
    private final DynamoDbClient dynamoDbClient;
    private final CloudWatchClient cloudWatchClient;

    public KinesisAdminService(
            @Value("${aws.region.static:us-east-1}") String region,
            @Value("${spring.cloud.aws.credentials.access-key:${aws_access_key_id:}}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key:${aws_secret_access_key:}}") String secretKey,
            @Value("${spring.cloud.aws.credentials.session-token:${aws_session_token:}}") String sessionToken) {

        Region awsRegion = Region.of(region);
        AwsCredentialsProvider credentials = obtenerCredenciales(accessKey, secretKey, sessionToken);

        this.kinesisClient = KinesisClient.builder().region(awsRegion).credentialsProvider(credentials).build();
        this.dynamoDbClient = DynamoDbClient.builder().region(awsRegion).credentialsProvider(credentials).build();
        this.cloudWatchClient = CloudWatchClient.builder().region(awsRegion).credentialsProvider(credentials).build();
    }

    private AwsCredentialsProvider obtenerCredenciales(String accessKey, String secretKey, String sessionToken) {
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

    public Map<String, Object> crearStream(String streamName, int shardCount) {
        kinesisClient.createStream(CreateStreamRequest.builder()
                .streamName(streamName)
                .shardCount(shardCount)
                .build());
        return Map.of("mensaje", "Stream en creación", "streamName", streamName, "shards", shardCount);
    }

    public List<String> listarStreams() {
        return kinesisClient.listStreams().streamNames();
    }

    public Map<String, Object> describirStream(String streamName) {
        StreamDescription desc = kinesisClient.describeStream(
                DescribeStreamRequest.builder().streamName(streamName).build()
        ).streamDescription();

        var shardsInfo = desc.shards().stream().map(s -> Map.of(
                "shardId", s.shardId(),
                "startingHashKey", s.hashKeyRange().startingHashKey(),
                "endingHashKey", s.hashKeyRange().endingHashKey()
        )).toList();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("streamName", desc.streamName());
        resultado.put("streamARN", desc.streamARN());
        resultado.put("status", desc.streamStatusAsString());
        resultado.put("retentionHours", desc.retentionPeriodHours());
        resultado.put("openShardCount", desc.shards().size());
        resultado.put("shards", shardsInfo);
        return resultado;
    }

    public Map<String, Object> escalarShards(String streamName, int targetShardCount) {
        UpdateShardCountResponse res = kinesisClient.updateShardCount(UpdateShardCountRequest.builder()
                .streamName(streamName)
                .targetShardCount(targetShardCount)
                .scalingType(ScalingType.UNIFORM_SCALING)
                .build());

        return Map.of(
                "streamName", res.streamName(),
                "currentShardCount", res.currentShardCount(),
                "targetShardCount", res.targetShardCount()
        );
    }

    public Map<String, Object> actualizarRetencion(String streamName, int retentionHours) {
        StreamDescription desc = kinesisClient.describeStream(
                DescribeStreamRequest.builder().streamName(streamName).build()
        ).streamDescription();
        int actual = desc.retentionPeriodHours();

        if (retentionHours > actual) {
            kinesisClient.increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest.builder()
                    .streamName(streamName)
                    .retentionPeriodHours(retentionHours)
                    .build());
        } else if (retentionHours < actual) {
            kinesisClient.decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder()
                    .streamName(streamName)
                    .retentionPeriodHours(retentionHours)
                    .build());
        }
        return Map.of("streamName", streamName, "retentionHours", retentionHours);
    }

    public Map<String, Object> eliminarStream(String streamName) {
        kinesisClient.deleteStream(DeleteStreamRequest.builder()
                .streamName(streamName)
                .enforceConsumerDeletion(true)
                .build());
        return Map.of("mensaje", "Stream eliminado", "streamName", streamName);
    }

    public Map<String, Object> inspeccionarConsumidor(String applicationName, String streamName) {
        ScanResponse scan = dynamoDbClient.scan(ScanRequest.builder()
                .tableName(applicationName)
                .build());

        var checkpointsPorShard = new ArrayList<>();
        for (Map<String, AttributeValue> item : scan.items()) {
            Map<String, String> lease = new LinkedHashMap<>();
            lease.put("shardId", item.containsKey("leaseKey") ? item.get("leaseKey").s() : "N/A");
            lease.put("leaseOwner", item.containsKey("leaseOwner") ? item.get("leaseOwner").s() : "SIN_ASIGNAR");
            lease.put("checkpoint", item.containsKey("checkpoint") ? item.get("checkpoint").s() : "N/A");
            lease.put("leaseCounter", item.containsKey("leaseCounter") ? item.get("leaseCounter").n() : "0");
            checkpointsPorShard.add(lease);
        }

        Double iteratorAgeMs = obtenerRetrasoCloudWatch(streamName);

        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("applicationName", applicationName);
        estado.put("streamName", streamName);
        estado.put("iteratorAgeMilliseconds", iteratorAgeMs);
        estado.put("shardsAsignados", checkpointsPorShard);
        return estado;
    }

    private Double obtenerRetrasoCloudWatch(String streamName) {
        try {
            GetMetricStatisticsResponse stats = cloudWatchClient.getMetricStatistics(
                    GetMetricStatisticsRequest.builder()
                            .namespace("AWS/Kinesis")
                            .metricName("GetRecords.IteratorAgeMilliseconds")
                            .dimensions(Dimension.builder().name("StreamName").value(streamName).build())
                            .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                            .endTime(Instant.now())
                            .period(60)
                            .statistics(Statistic.MAXIMUM)
                            .build()
            );
            return stats.datapoints().stream()
                    .max(Comparator.comparing(Datapoint::timestamp))
                    .map(Datapoint::maximum)
                    .orElse(0.0);
        } catch (Exception e) {
            return -1.0;
        }
    }
}