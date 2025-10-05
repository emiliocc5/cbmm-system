package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostgresTransactionRepository extends JpaRepository<Transaction,String> {
}
