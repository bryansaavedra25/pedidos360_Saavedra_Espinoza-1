package com.pedidos360.backend.service;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(String id) {
        super("No existe un producto asociado al ID: " + id);
    }
}