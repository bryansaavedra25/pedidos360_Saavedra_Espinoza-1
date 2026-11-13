package com.pedidos360.backend.messaging.facturacion;

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
public class FacturacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FacturacionConsumer.class);

    private final ProcesadorMensajes procesador;

    @Value("${aws.sqs.queues.facturacion-dlq}")
    private String dlq;

    public FacturacionConsumer(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    @SqsListener(queueNames = "${aws.sqs.queues.facturacion}", acknowledgementMode = "MANUAL")
    public void recibir(Message<String> message, Acknowledgement ack) {
        procesador.procesar("facturacion", dlq, message, ack, (eventType, pedido) ->
                log.info("Facturando ({}): cobro de {} al cliente {}",
                        eventType, pedido.getTotal(), pedido.getCliente()));
    }
}