package com.pedidos360.backend.messaging.kinesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.backend.messaging.EventoPedidoDTO;
import com.pedidos360.backend.messaging.PedidoEventPublisher;
import com.pedidos360.backend.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PedidoKinesisRecordProcessor implements ShardRecordProcessor {

    private static final Logger log = LoggerFactory.getLogger(PedidoKinesisRecordProcessor.class);
    private static final int MAX_REINTENTOS = 3;

    // Control de idempotencia por consumidor (clave: consumidor + ":" + eventId)
    private static final Set<String> EVENTOS_PROCESADOS = ConcurrentHashMap.newKeySet();

    private final String nombreConsumidor;
    private final ObjectMapper objectMapper;
    private final PedidoEventPublisher publisher;
    private String shardId;

    public PedidoKinesisRecordProcessor(String nombreConsumidor,
                                        ObjectMapper objectMapper,
                                        PedidoEventPublisher publisher) {
        this.nombreConsumidor = nombreConsumidor;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.shardId();
        log.info("[{}] Inicializado procesador para shardId={}", nombreConsumidor, shardId);
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        for (KinesisClientRecord record : processRecordsInput.records()) {
            String rawPayload = StandardCharsets.UTF_8.decode(record.data()).toString();
            procesarConResiliencia(rawPayload, record.sequenceNumber());
        }

        // Guardar checkpoint en DynamoDB tras procesar el lote sin bloquear el shard
        try {
            processRecordsInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.warn("[{}] No se pudo guardar checkpoint en shardId={} causa={}",
                    nombreConsumidor, shardId, e.getMessage());
        }
    }

    private void procesarConResiliencia(String rawPayload, String sequenceNumber) {
        EventoPedidoDTO evento;
        String eventId = "SIN_ID";

        // 1. Validación estructural del JSON (Error irrecuperable -> directo a DLQ)
        try {
            evento = objectMapper.readValue(rawPayload, EventoPedidoDTO.class);
            if (evento.getEventId() == null || evento.getPayload() == null) {
                throw new IllegalArgumentException("Payload JSON incompleto: falta eventId o payload");
            }
            eventId = evento.getEventId();
        } catch (Exception e) {
            log.error("[{}] Error irrecuperable de formato id={} intento=1 causa={}",
                    nombreConsumidor, eventId, e.getMessage());
            publisher.publicarEnStreamFallidos(eventId, rawPayload, "JSON_INVALIDO: " + e.getMessage(), nombreConsumidor);
            return;
        }

        // 2. Idempotencia: evitar doble procesamiento tras relectura de checkpoint
        String claveIdempotencia = nombreConsumidor + ":" + eventId;
        if (!EVENTOS_PROCESADOS.add(claveIdempotencia)) {
            log.info("[{}] Evento duplicado omitido por idempotencia id={} sequenceNumber={}",
                    nombreConsumidor, eventId, sequenceNumber);
            return;
        }

        // 3. Ejecución con reintentos ante fallos transitorios
        for (int intento = 1; intento <= MAX_REINTENTOS; intento++) {
            try {
                ejecutarLogicaNegocio(evento);
                log.info("[{}] Evento procesado OK id={} tipo={} intento={} shardId={}",
                        nombreConsumidor, eventId, evento.getEventType(), intento, shardId);
                return;
            } catch (IllegalArgumentException errorIrrecuperable) {
                // Error de validación de negocio: no tiene sentido reintentar
                log.error("[{}] Fallo irrecuperable id={} intento={} causa={}",
                        nombreConsumidor, eventId, intento, errorIrrecuperable.getMessage());
                publisher.publicarEnStreamFallidos(eventId, rawPayload, errorIrrecuperable.getMessage(), nombreConsumidor);
                return;
            } catch (Exception errorTransitorio) {
                log.warn("[{}] Error transitorio procesando id={} intento={}/{} causa={}",
                        nombreConsumidor, eventId, intento, MAX_REINTENTOS, errorTransitorio.getMessage());

                if (intento == MAX_REINTENTOS) {
                    log.error("[{}] Reintentos agotados. Derivando a stream de fallidos id={} intento={} causa={}",
                            nombreConsumidor, eventId, intento, errorTransitorio.getMessage());
                    publisher.publicarEnStreamFallidos(eventId, rawPayload,
                            "MAX_REINTENTOS_SUPERADOS: " + errorTransitorio.getMessage(), nombreConsumidor);
                } else {
                    try {
                        Thread.sleep(300L * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void ejecutarLogicaNegocio(EventoPedidoDTO evento) {
        Pedido pedido = evento.getPayload();

        // Regla de validación para demo de flujo fallido:
        // Si el total es <= 0 o el cliente dice "ERROR_TEST", lanzamos excepción irrecuperable
        if (pedido.getTotal() == null || pedido.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monto de pedido inválido (<= 0) para pedidoId=" + pedido.getId());
        }
        if ("SIMULAR_ERROR_TRANSITORIO".equalsIgnoreCase(pedido.getCliente())) {
            throw new RuntimeException("Fallo temporal de conexión con servicio externo");
        }

        if (nombreConsumidor.contains("facturacion")) {
            log.info("  --> [FACTURACIÓN] Generando comprobante para pedidoId={} cliente={} monto={}",
                    pedido.getId(), pedido.getCliente(), pedido.getTotal());
        } else {
            log.info("  --> [NOTIFICACIONES] Enviando aviso a cliente={} estado={}",
                    pedido.getCliente(), pedido.getEstado());
        }
    }

    @Override
    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.warn("[{}] Lease perdido para shardId={} (otra instancia asumió el shard)", nombreConsumidor, shardId);
    }

    @Override
    public void shardEnded(ShardEndedInput shardEndedInput) {
        try {
            log.info("[{}] Shard finalizado shardId={}, guardando checkpoint final", nombreConsumidor, shardId);
            shardEndedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.error("[{}] Error en checkpoint final shardId={} causa={}", nombreConsumidor, shardId, e.getMessage());
        }
    }

    @Override
    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        try {
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.error("[{}] Error guardando checkpoint al apagar causa={}", nombreConsumidor, e.getMessage());
        }
    }
}