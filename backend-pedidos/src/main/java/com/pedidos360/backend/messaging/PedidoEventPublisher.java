package com.pedidos360.backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.pedidos-topic-arn}")
    private String topicArn;

    public PedidoEventPublisher(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void publicar(String eventType, Pedido pedido) {
        try {
            String cuerpo = objectMapper.writeValueAsString(pedido);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(cuerpo)
                    .messageAttributes(Map.of("eventType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(eventType)
                                    .build()))
                    .build();
            PublishResponse respuesta = snsClient.publish(request);
            log.info("Evento publicado eventType={} messageId={}", eventType, respuesta.messageId());
        } catch (Exception e) {
            // Un fallo de mensajería no debe tumbar la respuesta HTTP del pedido
            log.error("No se pudo publicar eventType={} causa={}", eventType, e.getMessage());
        }
    }
}