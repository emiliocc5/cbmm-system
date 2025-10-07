package com.processor.core.use_case;

import com.processor.core.domain.enums.TransactionStatus;
import com.processor.core.domain.enums.TransactionType;
import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.model.Account;
import com.processor.core.domain.model.Transaction;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransferAccount;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.AccountRepository;
import com.processor.core.ports.out.TransactionRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class ProcessCbmmTransactionUseCaseImpl implements ProcessCbmmTransactionUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void process(TransactionData transaction) {
        List<String> accountIds = getSortedAccountIds(
                transaction.getSourceAccount().getAccountId(),
                transaction.getDestinationAccount().getAccountId()
        );

        Account account1 = accountRepository.findById(accountIds.get(0))
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountIds.getFirst()));

        Account account2 = accountIds.size() > 1
                ? accountRepository.findById(accountIds.get(1))
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountIds.get(1)))
                : account1;


        Account sourceAccount = account1.getId().equals(transaction.getSourceAccount().getAccountId())
                ? account1 : account2;
        Account destinationAccount = account1.getId().equals(transaction.getDestinationAccount().getAccountId())
                ? account1 : account2;

        validateTransactionCurrency(sourceAccount,
                transaction.getSourceAccount().getCurrency());
        validateTransactionCurrency(destinationAccount,
                transaction.getDestinationAccount().getCurrency());

        if (sourceAccount.getBalance()
                .compareTo(transaction.getSourceAccount().getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Source account has insufficient balance for event " + transaction.getEventId());
        }

        sourceAccount.debit(transaction.getSourceAccount().getAmount());
        destinationAccount.credit(transaction.getDestinationAccount().getAmount());

        Transaction debitTransaction = buildTransaction(transaction.getSourceAccount(), TransactionType.DEBIT,
                transaction.getEventId(), transaction.getOperationDate(),
                sourceAccount.getBalance(), TransactionStatus.APPLIED);

        Transaction creditTransaction = buildTransaction(transaction.getDestinationAccount(), TransactionType.CREDIT,
                transaction.getEventId(), transaction.getOperationDate(),
                destinationAccount.getBalance(), TransactionStatus.APPLIED);


        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        entityManager.flush();

        log.info("Transaction processed successfully: {} -> {}",
                sourceAccount.getId(),
                destinationAccount.getId());
    }

    private Transaction buildTransaction(TransferAccount account, TransactionType type,
                                         String eventId, LocalDateTime operationDate,
                                         BigDecimal balanceAfter, TransactionStatus status) {
        return Transaction.builder()
                .id(getUUID())
                .accountId(account.getAccountId())
                .eventId(eventId)
                .type(type)
                .amount(account.getAmount())
                .currency(account.getCurrency())
                .balanceAfter(balanceAfter)
                .operationDate(operationDate)
                .processedAt(LocalDateTime.now())
                .status(status)
                .build();
    }

    private List<String> getSortedAccountIds(String sourceId, String destinationId) {
        if (sourceId.equals(destinationId)) {
            return List.of(sourceId);
        }
        return Stream.of(sourceId, destinationId)
                .sorted()
                .toList();
    }

    private void validateTransactionCurrency(Account account, String currency) {
        if (!account.getCurrency().equals(currency)) {
            throw new InvalidCurrencyException("Currency mismatch for account " + account.getId() +
                    ". Expected: " + account.getCurrency() + ", Got: " + currency);
        }
    }

    private String getUUID(){
        return UUID.randomUUID().toString();
    }
}
