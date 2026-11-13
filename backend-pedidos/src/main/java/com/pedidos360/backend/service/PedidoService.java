package com.pedidos360.backend.service;

import com.pedidos360.backend.messaging.PedidoEventPublisher;
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
    private final PedidoEventPublisher publisher;

    public PedidoService(PedidoRepository pedidoRepository, PedidoEventPublisher publisher) {
        this.pedidoRepository = pedidoRepository;
        this.publisher = publisher;
    }

    @PostConstruct
    public void cargarDatosEjemplo() {
        if (pedidoRepository.count() == 0) {
            pedidoRepository.save(new Pedido(null, "Empresa Alfa Ltda.", new BigDecimal("150000.00"), "PROCESANDO"));
            pedidoRepository.save(new Pedido(null, "Juan Pérez", new BigDecimal("45990.00"), "COMPLETADO"));
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
        Pedido guardado = pedidoRepository.save(nuevo);
        publisher.publicar("PEDIDO_CREADO", guardado);
        return guardado;
    }

    public Pedido completar(String id) {
        Pedido pedido = buscarPorId(id);
        pedido.setEstado("COMPLETADO");
        Pedido guardado = pedidoRepository.save(pedido);
        publisher.publicar("PEDIDO_COMPLETADO", guardado);
        return guardado;
    }

    public void eliminar(String id) {
        Pedido pedido = buscarPorId(id);
        pedidoRepository.deleteById(id);
        publisher.publicar("PEDIDO_ELIMINADO", pedido);
    }
}