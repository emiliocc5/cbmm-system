package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Transaction;
import com.processor.core.ports.out.TransactionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {
    @Override
    public void save(Transaction transaction) {

    }
}
