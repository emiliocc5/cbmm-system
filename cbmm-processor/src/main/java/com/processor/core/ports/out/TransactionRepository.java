package com.processor.core.ports.out;

import com.processor.core.domain.model.Transaction;

public interface TransactionRepository {
    void save(Transaction transaction);
}
