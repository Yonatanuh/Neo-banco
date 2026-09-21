package com.neobanco.backend.repository;

import com.neobanco.backend.model.Suscripcion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SuscripcionRepository extends MongoRepository<Suscripcion, String> {
    List<Suscripcion> findByUsuario(String usuario);
    Suscripcion findByIdAndUsuario(String id, String usuario);
}
