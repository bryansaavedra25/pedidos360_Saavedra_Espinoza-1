package com.pedidos360.backend.service;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.repository.PedidoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @PostConstruct
    public void cargarDatosEjemplo() {
        if (pedidoRepository.count() == 0) {
            crear(new Pedido(null, "Empresa Alfa Ltda.", new BigDecimal("150000.00"), "PROCESANDO"));
            crear(new Pedido(null, "Juan Pérez", new BigDecimal("45990.00"), "COMPLETADO"));
        }
    }

    public List<Pedido> listarTodos() {
        List<Pedido> lista = new ArrayList<>();
        pedidoRepository.findAll().forEach(lista::add);
        return lista;
    }

    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    public Pedido crear(Pedido nuevo) {
        return pedidoRepository.save(nuevo);
    }

    public void eliminar(String id) {
        if (!pedidoRepository.existsById(id)) {
            throw new PedidoNoEncontradoException(id);
        }
        pedidoRepository.deleteById(id);
    }
}