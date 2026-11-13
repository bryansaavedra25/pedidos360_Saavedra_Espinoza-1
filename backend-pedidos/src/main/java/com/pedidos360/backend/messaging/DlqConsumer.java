package com.pedidos360.backend.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "app.mensajeria", name = "consumidores", havingValue = "true", matchIfMissing = true)
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    @SqsListener(queueNames = {
            "${aws.sqs.queues.notificaciones-dlq}",
            "${aws.sqs.queues.facturacion-dlq}",
            "${aws.sqs.queues.inventario-dlq}"
    }, acknowledgementMode = "MANUAL")
    public void registrar(Message<String> message) {
        Object intento = message.getHeaders().get("Sqs_Msa_ApproximateReceiveCount");
        Object causa = message.getHeaders().get("causa");
        log.error("[DLQ] id={} intento={} causa={} cuerpo={}",
                message.getHeaders().getId(),
                intento == null ? "?" : intento,
                causa == null ? "maxReceiveCount agotado" : causa,
                message.getPayload());
    }
}