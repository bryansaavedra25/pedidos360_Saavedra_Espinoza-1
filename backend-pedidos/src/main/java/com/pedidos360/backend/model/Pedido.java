package com.pedidos360.backend.model;

import com.arangodb.springframework.annotation.Document;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Document("pedidos")
public class Pedido {

    @Id
    private String id; // ArangoDB asigna un hash/clave única de tipo String automáticamente

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String cliente;

    @NotNull(message = "El total es obligatorio")
    @Min(value = 1, message = "El total del pedido debe ser mayor a 0")
    private BigDecimal total;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    public Pedido() {}

    public Pedido(String id, String cliente, BigDecimal total, String estado) {
        this.id = id;
        this.cliente = cliente;
        this.total = total;
        this.estado = estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}