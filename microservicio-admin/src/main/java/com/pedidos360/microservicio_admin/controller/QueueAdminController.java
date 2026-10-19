package com.pedidos360.microservicio_admin.controller;

import com.pedidos360.microservicio_admin.service.ColaAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class QueueAdminController {

    private final ColaAdminService colaAdminService;

    public QueueAdminController(ColaAdminService colaAdminService) {
        this.colaAdminService = colaAdminService;
    }

    @PostMapping("/queues")
    public ResponseEntity<String> crearCola(@RequestParam String name) {
        return ResponseEntity.ok(colaAdminService.crearCola(name));
    }

    @DeleteMapping("/queues")
    public ResponseEntity<Void> eliminarCola(@RequestParam String url) {
        colaAdminService.eliminarCola(url);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/topics")
    public ResponseEntity<String> crearTopico(@RequestParam String name) {
        return ResponseEntity.ok(colaAdminService.crearTopico(name));
    }

    @GetMapping("/queues/status")
    public ResponseEntity<Map<String, String>> obtenerEstado(@RequestParam String url) {
        return ResponseEntity.ok(colaAdminService.obtenerEstadoCola(url));
    }

    @PostMapping("/dlq/purge")
    public ResponseEntity<Void> purgarDlq(@RequestParam String url) {
        colaAdminService.purgarCola(url);
        return ResponseEntity.noContent().build();
    }
}