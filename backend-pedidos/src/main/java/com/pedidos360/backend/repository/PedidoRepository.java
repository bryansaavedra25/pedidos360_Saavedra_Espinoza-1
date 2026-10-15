package com.pedidos360.backend.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.pedidos360.backend.model.Pedido;
import org.springframework.stereotype.Repository;

//Interface para las operaciones CRUD en ArangoDB
@Repository
public interface PedidoRepository extends ArangoRepository<Pedido, String> {  
}
