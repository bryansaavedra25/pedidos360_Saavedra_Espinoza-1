package com.pedidos360.backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

import java.util.Map;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final KinesisClient kinesisClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.kinesis.streams.pedidos:pedidos-stream}")
    private String streamPedidos;

    @Value("${aws.kinesis.streams.dlq:mensajes-fallidos-stream}")
    private String streamDlq;

    public PedidoEventPublisher(KinesisClient kinesisClient, ObjectMapper objectMapper) {
        this.kinesisClient = kinesisClient;
        this.objectMapper = objectMapper;
    }

    public void publicar(String eventType, Pedido pedido) {
        EventoPedidoDTO evento = new EventoPedidoDTO(eventType, pedido);
        try {
            String cuerpoJson = objectMapper.writeValueAsString(evento);

            // Partition Key justificada: ID del pedido para garantizar el orden cronológico
            // de eventos (CREADO -> COMPLETADO -> ELIMINADO) dentro del mismo shard.
            String partitionKey = (pedido != null && pedido.getId() != null)
                    ? pedido.getId()
                    : evento.getEventId();

            PutRecordRequest request = PutRecordRequest.builder()
                    .streamName(streamPedidos)
                    .partitionKey(partitionKey)
                    .data(SdkBytes.fromUtf8String(cuerpoJson))
                    .build();

            PutRecordResponse respuesta = kinesisClient.putRecord(request);

            log.info("Evento publicado en Kinesis stream={} eventId={} eventType={} shardId={} sequenceNumber={}",
                    streamPedidos, evento.getEventId(), eventType, respuesta.shardId(), respuesta.sequenceNumber());

        } catch (Exception e) {
            log.error("Error publicando en Kinesis eventId={} eventType={} causa={}",
                    evento.getEventId(), eventType, e.getMessage());
        }
    }

    /**
     * Envía eventos irrecuperables al Stream de Mensajes Fallidos (DLQ)
     */
    public void publicarEnStreamFallidos(String eventId, String payloadOriginal, String causaError, String consumidor) {
        try {
            Map<String, Object> registroFallo = Map.of(
                    "eventId", eventId != null ? eventId : "DESCONOCIDO",
                    "consumidor", consumidor,
                    "causa", causaError,
                    "payloadOriginal", payloadOriginal
            );
            String jsonFallo = objectMapper.writeValueAsString(registroFallo);

            PutRecordRequest request = PutRecordRequest.builder()
                    .streamName(streamDlq)
                    .partitionKey(eventId != null ? eventId : "dlq-key")
                    .data(SdkBytes.fromUtf8String(jsonFallo))
                    .build();

            kinesisClient.putRecord(request);
            log.warn("Evento derivado a stream de fallidos={} eventId={} consumidor={} causa={}",
                    streamDlq, eventId, consumidor, causaError);
        } catch (Exception ex) {
            log.error("Fallo crítico al enviar a stream de fallidos eventId={} causa={}", eventId, ex.getMessage());
        }
    }
}