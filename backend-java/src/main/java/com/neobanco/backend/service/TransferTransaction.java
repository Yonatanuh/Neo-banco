package com.neobanco.backend.service;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;

/** Keeps a simulated transfer, its ledger entries and rounding in one MongoDB transaction. */
@Service
public class TransferTransaction {
    private final TransactionTemplate transaction;

    public TransferTransaction(MongoTemplate mongo) {
        this.transaction = new TransactionTemplate(new MongoTransactionManager(mongo.getMongoDatabaseFactory()));
    }

    public ResponseEntity<?> execute(Supplier<ResponseEntity<?>> operation) {
        return transaction.execute(status -> operation.get());
    }
}
