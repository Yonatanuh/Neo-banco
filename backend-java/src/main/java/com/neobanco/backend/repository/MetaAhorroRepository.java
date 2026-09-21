package com.neobanco.backend.repository;

import com.neobanco.backend.model.MetaAhorro;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MetaAhorroRepository extends MongoRepository<MetaAhorro, String> {
    List<MetaAhorro> findByUsuario(String usuario);
}
