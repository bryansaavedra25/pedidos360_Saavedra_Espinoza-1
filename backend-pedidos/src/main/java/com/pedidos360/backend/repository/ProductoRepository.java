package com.pedidos360.backend.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.pedidos360.backend.model.Producto;
import org.springframework.stereotype.Repository;

// Interface para las operaciones CRUD de Productos en ArangoDB
@Repository
public interface ProductoRepository extends ArangoRepository<Producto, String> {
}