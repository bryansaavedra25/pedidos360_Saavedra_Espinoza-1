package com.pedidos360.backend.model;

import com.arangodb.springframework.annotation.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Document("mensajes_procesados")
public class MensajeProcesado {

    @Id
    private String id;
    private String procesadoEn;

    public MensajeProcesado() {}

    public MensajeProcesado(String id) {
        this.id = id;
        this.procesadoEn = Instant.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProcesadoEn() { return procesadoEn; }
    public void setProcesadoEn(String procesadoEn) { this.procesadoEn = procesadoEn; }
}