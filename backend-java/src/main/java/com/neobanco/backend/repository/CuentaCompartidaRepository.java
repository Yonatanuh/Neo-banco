package com.neobanco.backend.repository;

import com.neobanco.backend.model.CuentaCompartida;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CuentaCompartidaRepository extends MongoRepository<CuentaCompartida, String> {
    List<CuentaCompartida> findByMiembrosContaining(String usuarioId);
    Optional<CuentaCompartida> findByIdAndMiembrosContaining(String id, String usuarioId);
}
