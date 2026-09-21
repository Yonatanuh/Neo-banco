package com.neobanco.backend.repository;

import com.neobanco.backend.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByUsuarioOrderByCreatedAtDesc(String usuario);
    Ticket findByIdAndUsuario(String id, String usuario);
}

