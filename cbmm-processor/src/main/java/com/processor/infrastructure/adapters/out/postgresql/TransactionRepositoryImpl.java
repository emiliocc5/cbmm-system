package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Transaction;
import com.processor.core.ports.out.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
    private final PostgresTransactionRepository postgresTransactionRepository;

    @Override
    public void save(Transaction transaction) {
        postgresTransactionRepository.save(transaction);
    }
}
