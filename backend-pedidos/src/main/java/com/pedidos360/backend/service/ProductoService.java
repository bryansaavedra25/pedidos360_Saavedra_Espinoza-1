package com.pedidos360.backend.service;

import com.pedidos360.backend.model.Producto;
import com.pedidos360.backend.repository.ProductoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @PostConstruct
    public void cargarDatosEjemplo() {
        if (productoRepository.count() == 0) {
            crear(new Producto(null, "Teclado Mecánico RGB", new BigDecimal("45990.00"), 15));
            crear(new Producto(null, "Mouse Inalámbrico", new BigDecimal("19990.00"), 30));
        }
    }

    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<>();
        productoRepository.findAll().forEach(lista::add);
        return lista;
    }

    public Producto buscarPorId(String id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    public Producto crear(Producto nuevo) {
        return productoRepository.save(nuevo);
    }

    public void eliminar(String id) {
        if (!productoRepository.existsById(id)) {
            throw new ProductoNoEncontradoException(id);
        }
        productoRepository.deleteById(id);
    }
}