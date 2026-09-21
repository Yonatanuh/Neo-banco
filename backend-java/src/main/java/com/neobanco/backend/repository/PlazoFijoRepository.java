package com.neobanco.backend.repository;

import com.neobanco.backend.model.PlazoFijo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PlazoFijoRepository extends MongoRepository<PlazoFijo, String> {
    List<PlazoFijo> findByUsuarioOrderByCreatedAtDesc(String usuario);
    Optional<PlazoFijo> findByIdAndUsuario(String id, String usuario);
}
