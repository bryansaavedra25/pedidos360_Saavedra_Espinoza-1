package com.pedidos360.backend.messaging.kinesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.messaging.PedidoEventPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class KinesisConsumersRunner {

    private static final Logger log = LoggerFactory.getLogger(KinesisConsumersRunner.class);

    private final KinesisAsyncClient kinesisAsyncClient;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    private final ObjectMapper objectMapper;
    private final PedidoEventPublisher publisher;

    @Value("${aws.kinesis.streams.pedidos:pedidos-stream}")
    private String streamPedidos;

    @Value("${aws.kinesis.consumer-groups.facturacion:pedidos360-facturacion-consumer}")
    private String appFacturacion;

    @Value("${aws.kinesis.consumer-groups.notificaciones:pedidos360-notificaciones-consumer}")
    private String appNotificaciones;

    private final List<Scheduler> schedulers = new ArrayList<>();

    public KinesisConsumersRunner(KinesisAsyncClient kinesisAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  CloudWatchAsyncClient cloudWatchAsyncClient,
                                  ObjectMapper objectMapper,
                                  PedidoEventPublisher publisher) {
        this.kinesisAsyncClient = kinesisAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    @PostConstruct
    public void iniciarConsumidores() {
        iniciarWorkerKcl(appFacturacion);
        iniciarWorkerKcl(appNotificaciones);
    }

    private void iniciarWorkerKcl(String applicationName) {
        String workerId = applicationName + "-" + UUID.randomUUID();
        ShardRecordProcessorFactory factory = () ->
                new PedidoKinesisRecordProcessor(applicationName, objectMapper, publisher);

        ConfigsBuilder configsBuilder = new ConfigsBuilder(
                streamPedidos,
                applicationName,
                kinesisAsyncClient,
                dynamoDbAsyncClient,
                cloudWatchAsyncClient,
                workerId,
                factory
        );

        Scheduler scheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig()
        );

        schedulers.add(scheduler);
        Thread hiloWorker = new Thread(scheduler, "kcl-worker-" + applicationName);
        hiloWorker.setDaemon(true);
        hiloWorker.start();

        log.info("Iniciado consumidor KCL applicationName={} workerId={} stream={}",
                applicationName, workerId, streamPedidos);
    }

    @PreDestroy
    public void detenerConsumidores() {
        for (Scheduler scheduler : schedulers) {
            scheduler.shutdown();
        }
    }
}