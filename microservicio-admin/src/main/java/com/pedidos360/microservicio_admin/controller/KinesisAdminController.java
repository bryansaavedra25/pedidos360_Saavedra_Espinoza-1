package com.pedidos360.microservicio_admin.controller;

import com.pedidos360.microservicio_admin.service.KinesisAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/kinesis")
@CrossOrigin(origins = "*")
public class KinesisAdminController {

    private final KinesisAdminService kinesisAdminService;

    public KinesisAdminController(KinesisAdminService kinesisAdminService) {
        this.kinesisAdminService = kinesisAdminService;
    }

    @PostMapping("/streams")
    public ResponseEntity<?> crearStream(
            @RequestParam String name,
            @RequestParam(defaultValue = "3") int shards) {
        return ResponseEntity.ok(kinesisAdminService.crearStream(name, shards));
    }

    @GetMapping("/streams")
    public ResponseEntity<List<String>> listarStreams() {
        return ResponseEntity.ok(kinesisAdminService.listarStreams());
    }

    @GetMapping("/streams/{name}")
    public ResponseEntity<?> describirStream(@PathVariable String name) {
        return ResponseEntity.ok(kinesisAdminService.describirStream(name));
    }

    @PutMapping("/streams/{name}/shards")
    public ResponseEntity<?> escalarShards(
            @PathVariable String name,
            @RequestParam int targetShards) {
        return ResponseEntity.ok(kinesisAdminService.escalarShards(name, targetShards));
    }

    @PutMapping("/streams/{name}/retention")
    public ResponseEntity<?> actualizarRetencion(
            @PathVariable String name,
            @RequestParam int hours) {
        return ResponseEntity.ok(kinesisAdminService.actualizarRetencion(name, hours));
    }

    @DeleteMapping("/streams/{name}")
    public ResponseEntity<?> eliminarStream(@PathVariable String name) {
        return ResponseEntity.ok(kinesisAdminService.eliminarStream(name));
    }

    @GetMapping("/consumers/{appName}")
    public ResponseEntity<?> inspeccionarConsumidor(
            @PathVariable String appName,
            @RequestParam(defaultValue = "pedidos-stream") String streamName) {
        return ResponseEntity.ok(kinesisAdminService.inspeccionarConsumidor(appName, streamName));
    }
}