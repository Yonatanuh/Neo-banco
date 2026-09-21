package com.neobanco.backend.repository;

import com.neobanco.backend.model.TransferenciaSWIFT;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferenciaSWIFTRepository extends MongoRepository<TransferenciaSWIFT, String> {
    List<TransferenciaSWIFT> findByRemitenteOrderByCreatedAtDesc(String remitente);
}

