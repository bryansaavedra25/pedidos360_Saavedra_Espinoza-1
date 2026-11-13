package com.pedidos360.backend.messaging.inventario;

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
public class InventarioConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioConsumer.class);

    private final ProcesadorMensajes procesador;

    @Value("${aws.sqs.queues.inventario-dlq}")
    private String dlq;

    public InventarioConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @SqsListener(queueNames = "${aws.sqs.queues.inventario}", acknowledgementMode = "MANUAL")
    public void recibir(Message<String> message, Acknowledgement ack) {
        procesador.procesar("inventario", dlq, message, ack, (eventType, pedido) -> {
            if ("PEDIDO_ELIMINADO".equals(eventType)) {
                log.info("Inventario: reponiendo stock del pedido {}", pedido.getId());
            } else {
                log.info("Inventario: descontando stock del pedido {}", pedido.getId());
            }
        });
    }
}