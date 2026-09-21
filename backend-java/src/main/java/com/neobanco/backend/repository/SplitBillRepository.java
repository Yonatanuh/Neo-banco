package com.neobanco.backend.repository;

import com.neobanco.backend.model.SplitBill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitBillRepository extends MongoRepository<SplitBill, String> {
    List<SplitBill> findByCreadorOrParticipantesUsuario(String creadorId, String participanteId);
}

