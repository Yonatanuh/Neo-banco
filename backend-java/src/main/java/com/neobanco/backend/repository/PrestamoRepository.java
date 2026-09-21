package com.neobanco.backend.repository;

import com.neobanco.backend.model.Prestamo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PrestamoRepository extends MongoRepository<Prestamo, String> {
    List<Prestamo> findByUsuarioOrderByCreatedAtDesc(String usuario);
    List<Prestamo> findByUsuarioAndEstado(String usuario, String estado);
    List<Prestamo> findByEstado(String estado);
}
