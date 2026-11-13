package com.pedidos360.backend.messaging;

import com.pedidos360.backend.model.Pedido;
import java.time.Instant;
import java.util.UUID;

public class EventoPedidoDTO {

    private String eventId;
    private String eventType;
    private String timestamp;
    private Pedido payload;

    public EventoPedidoDTO() {
    }

    public EventoPedidoDTO(String eventType, Pedido payload) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now().toString();
        this.payload = payload;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Pedido getPayload() { return payload; }
    public void setPayload(Pedido payload) { this.payload = payload; }
}