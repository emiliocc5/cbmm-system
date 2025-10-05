package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Account;
import com.processor.core.ports.out.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
    private final PostgresAccountRepository postgresAccountRepository;

    @Override
    public Optional<Account> findById(String accountId) {
        return postgresAccountRepository.findById(accountId);
    }

    @Override
    public void save(Account account) {
        postgresAccountRepository.save(account);
    }
}
