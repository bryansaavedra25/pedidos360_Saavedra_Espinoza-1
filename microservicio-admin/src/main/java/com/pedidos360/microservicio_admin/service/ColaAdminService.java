package com.pedidos360.microservicio_admin.service;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class ColaAdminService {

    private final SqsClient sqsClient;
    private final SnsClient snsClient;

    public ColaAdminService() {
        this.sqsClient = SqsClient.builder().region(Region.US_EAST_1).build();
        this.snsClient = SnsClient.builder().region(Region.US_EAST_1).build();
    }

    public String crearCola(String queueName) {
        CreateQueueRequest request = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();
        return sqsClient.createQueue(request).queueUrl();
    }

    public void eliminarCola(String queueUrl) {
        DeleteQueueRequest request = DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
        sqsClient.deleteQueue(request);
    }

    public String crearTopico(String topicName) {
        CreateTopicRequest request = CreateTopicRequest.builder()
                .name(topicName)
                .build();
        return snsClient.createTopic(request).topicArn();
    }

    public Map<String, String> obtenerEstadoCola(String queueUrl) {
        GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED
                ).build();

        GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
        Map<String, String> estado = new HashMap<>();
        estado.put("MensajesDisponibles", response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
        estado.put("MensajesEnProceso", response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
        estado.put("MensajesDiferidos", response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
        return estado;
    }

    public void purgarCola(String queueUrl) {
        PurgeQueueRequest request = PurgeQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
        sqsClient.purgeQueue(request);
    }
}