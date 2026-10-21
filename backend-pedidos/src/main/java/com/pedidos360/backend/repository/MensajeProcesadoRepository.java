package com.pedidos360.backend.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.pedidos360.backend.model.MensajeProcesado;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeProcesadoRepository extends ArangoRepository<MensajeProcesado, String> {

}