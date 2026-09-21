package com.neobanco.backend.repository;

import com.neobanco.backend.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findByEmailIn(List<String> emails);
}
