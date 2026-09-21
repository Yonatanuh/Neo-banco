package com.neobanco.backend.repository;

import com.neobanco.backend.model.Transaccion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface TransaccionRepository extends MongoRepository<Transaccion, String> {
    List<Transaccion> findByRemitenteIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(String remitenteId, java.time.LocalDateTime date);
    
    List<Transaccion> findByRemitenteIdAndTipoOrderByCreatedAtDesc(String remitenteId, String tipo);
    
    @Query("{ $or: [ { remitenteId: ?0 }, { destinatarioId: ?0 } ] }")
    List<Transaccion> findByUsuario(String usuario);

    @Query("{ $or: [ { remitenteId: ?0 }, { destinatarioId: ?0 }, { remitente: ?0 }, { destinatario: ?0 } ], fecha: { $gte: ?1 } }")
    List<Transaccion> findTransaccionesUsuarioDesde(String usuarioId, Date fecha);
}
