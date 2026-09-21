package com.neobanco.backend.repository;

import com.neobanco.backend.model.Contacto;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ContactoRepository extends MongoRepository<Contacto, String> {
    List<Contacto> findByUsuario(String usuario);
    Optional<Contacto> findByUsuarioAndEmailContacto(String usuario, String emailContacto);
}
