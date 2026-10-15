package com.pedidos360.backend.controller;

import com.pedidos360.backend.model.Pedido;
import com.pedidos360.backend.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService; // ayuda a introducir una dependencia del servicio de negocio

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping //Ayuda a obtener la lista completa
    public List<Pedido> listar() {
        return pedidoService.listarTodos(); 
    }

    @GetMapping("/{id}") //Aqui busca un pedido en especifico
    public Pedido obtener(@PathVariable String id) {
        return pedidoService.buscarPorId(id);
    }

    @PostMapping //Valida y guarda el nuevo pedido
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido crear(@Valid @RequestBody Pedido nuevo) {
        return pedidoService.crear(nuevo);
    }

    @DeleteMapping("/{id}") //Y aqui elimina el pedido por su ID
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        pedidoService.eliminar(id);
    }
}
