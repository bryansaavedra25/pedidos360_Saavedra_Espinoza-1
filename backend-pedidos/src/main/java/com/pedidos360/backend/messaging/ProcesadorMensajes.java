package com.pedidos360.backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.model.MensajeProcesado;
import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.repository.MensajeProcesadoRepository;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Component
public class ProcesadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorMensajes.class);
    private static final String HEADER_INTENTOS = "Sqs_Msa_ApproximateReceiveCount";

    private final ObjectMapper objectMapper;
    private final MensajeProcesadoRepository procesados;
    private final SqsAsyncClient sqs;

    public ProcesadorMensajes(ObjectMapper objectMapper,
                              MensajeProcesadoRepository procesados,
                              SqsAsyncClient sqs) {
        this.objectMapper = objectMapper;
        this.procesados = procesados;
        this.sqs = sqs;
    }

    @FunctionalInterface
    public interface Accion {
        void ejecutar(String eventType, Pedido pedido) throws Exception;
    }

    public void procesar(String dominio, String dlqNombre, Message<String> message,
                         Acknowledgement ack, Accion accion) {
        String id = String.valueOf(message.getHeaders().getId());
        Object intentoHeader = message.getHeaders().get(HEADER_INTENTOS);
        String intento = intentoHeader == null ? "?" : intentoHeader.toString();
        Object tipo = message.getHeaders().get("eventType");
        String eventType = tipo == null ? "DESCONOCIDO" : tipo.toString();
        String clave = dominio + "-" + id;

        // 1) Duplicados: si el messageId ya fue procesado, se confirma y se descarta
        if (procesados.existsById(clave)) {
            log.warn("[{}] id={} duplicado, se confirma y se descarta", dominio, id);
            ack.acknowledge();
            return;
        }

        // 2) Error NO recuperable: cuerpo ilegible -> directo a la DLQ, sin reintentos
        Pedido pedido;
        try {
            pedido = objectMapper.readValue(message.getPayload(), Pedido.class);
        } catch (Exception e) {
            log.error("[{}] id={} intento={} causa={} (no recuperable, va a la DLQ)",
                    dominio, id, intento, e.getMessage());
            enviarADlq(dlqNombre, message.getPayload(), id, e.getMessage());
            ack.acknowledge();
            return;
        }

        // 3) Procesamiento normal. Error recuperable: no se confirma y SQS reentrega
        try {
            // Fallo simulado SOLO para la demo de reintentos (cliente = FALLAR)
            if ("inventario".equals(dominio) && "FALLAR".equalsIgnoreCase(pedido.getCliente())) {
                throw new IllegalStateException("Fallo simulado para la demo");
            }
            accion.ejecutar(eventType, pedido);
            procesados.save(new MensajeProcesado(clave));
            ack.acknowledge();
            log.info("[{}] id={} eventType={} procesado correctamente (ACK)", dominio, id, eventType);
        } catch (Exception e) {
            log.warn("[{}] id={} intento={} causa={} (recuperable, sin ACK)",
                    dominio, id, intento, e.getMessage());
        }
    }

    private void enviarADlq(String dlqNombre, String cuerpo, String idOriginal, String causa) {
        String url = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(dlqNombre).build())
                .join().queueUrl();
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(cuerpo)
                .messageAttributes(Map.of(
                        "originalMessageId", atributo(idOriginal),
                        "causa", atributo(causa == null ? "desconocida" : causa)))
                .build()).join();
    }

    private MessageAttributeValue atributo(String valor) {
        return MessageAttributeValue.builder().dataType("String").stringValue(valor).build();
    }
}