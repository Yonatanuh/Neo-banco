package com.neobanco.backend.repository;

import com.neobanco.backend.model.Tarjeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaRepository extends MongoRepository<Tarjeta, String> {
    List<Tarjeta> findByUsuarioId(String usuarioId);
    // Since field is named numero_tarjeta we map exactly to that in the method name or use @Query. 
    // Spring Data can handle exact matches by property name
    Optional<Tarjeta> findByNumeroTarjeta(String numeroTarjeta);
}
