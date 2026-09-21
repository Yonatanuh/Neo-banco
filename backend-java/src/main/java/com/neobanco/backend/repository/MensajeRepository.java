package com.neobanco.backend.repository;

import com.neobanco.backend.model.Mensaje;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeRepository extends MongoRepository<Mensaje, String> {
    
    @Query("{ '$or': [ { 'remitente': ?0, 'destinatario': ?1 }, { 'remitente': ?1, 'destinatario': ?0 } ] }")
    List<Mensaje> findConversacion(String usuarioId1, String usuarioId2);

    @Query("{ 'remitente': ?0, 'destinatario': ?1, 'leido': false }")
    List<Mensaje> findUnreadMessages(String remitente, String destinatario);
    
    @Query("{ '$or': [ { 'remitente': ?0 }, { 'destinatario': ?0 } ] }")
    List<Mensaje> findByRemitenteOrDestinatario(String usuarioId);
}
