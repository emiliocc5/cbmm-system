package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostgresAccountRepository extends JpaRepository<Account,String> {
}
