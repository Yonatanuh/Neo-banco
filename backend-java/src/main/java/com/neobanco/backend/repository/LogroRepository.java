package com.neobanco.backend.repository;

import com.neobanco.backend.model.Logro;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LogroRepository extends MongoRepository<Logro, String> {
    List<Logro> findByUsuario(String usuario);
}
