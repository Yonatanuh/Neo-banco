package com.neobanco.backend.repository;

import com.neobanco.backend.model.Inversion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface InversionRepository extends MongoRepository<Inversion, String> {
    List<Inversion> findByUsuario(String usuario);
    Optional<Inversion> findByUsuarioAndCriptomoneda(String usuario, String criptomoneda);
}
