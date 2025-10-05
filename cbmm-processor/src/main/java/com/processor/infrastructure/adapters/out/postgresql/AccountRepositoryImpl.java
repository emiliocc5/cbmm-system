package com.processor.infrastructure.adapters.out.postgresql;

import com.processor.core.domain.model.Account;
import com.processor.core.ports.out.AccountRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepositoryImpl implements AccountRepository {
    @Override
    public Optional<Account> findById(String accountId) {
        return Optional.empty();
    }

    @Override
    public void save(Account account) {

    }
}
