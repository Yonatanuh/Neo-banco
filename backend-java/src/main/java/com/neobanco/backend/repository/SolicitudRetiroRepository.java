package com.neobanco.backend.repository;

import com.neobanco.backend.model.SolicitudRetiro;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SolicitudRetiroRepository extends MongoRepository<SolicitudRetiro, String> {
}
