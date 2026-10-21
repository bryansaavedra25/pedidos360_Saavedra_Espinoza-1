package com.pedidos360.backend.messaging.notificaciones;

import com.pedidos360.backend.messaging.ProcesadorMensajes;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "app.mensajeria", name = "consumidores", havingValue = "true", matchIfMissing = true)
@Component
public class NotificacionesConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesConsumer.class);

    private final ProcesadorMensajes procesador;

    @Value("${aws.sqs.queues.notificaciones-dlq}")
    private String dlq;

    // Solo para la demo de la diapositiva 8; en uso normal vale 0
    @Value("${app.demo.retraso-ms:0}")
    private long retrasoMs;

    public NotificacionesConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @SqsListener(queueNames = "${aws.sqs.queues.notificaciones}", acknowledgementMode = "MANUAL")
    public void recibir(Message<String> message, Acknowledgement ack) {
        procesador.procesar("notificaciones", dlq, message, ack, (eventType, pedido) -> {
            if (retrasoMs > 0) {
                log.info("Notificaciones: iniciando pedido {} (trabajo de {} ms)", pedido.getId(), retrasoMs);
                Thread.sleep(retrasoMs);
            }
            log.info("Notificando a {}: pedido {} con estado {}",
                    pedido.getCliente(), pedido.getId(), pedido.getEstado());
        });
    }
}