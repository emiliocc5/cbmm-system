package com.processor.core.use_case;

import com.processor.core.domain.enums.TransactionStatus;
import com.processor.core.domain.enums.TransactionType;
import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.exception.TransactionProcessingException;
import com.processor.core.domain.model.Account;
import com.processor.core.domain.model.Transaction;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.AccountRepository;
import com.processor.core.ports.out.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class ProcessCbmmTransactionUseCaseImpl implements ProcessCbmmTransactionUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    //TODO take this to environment variables
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;


    @Override
    @Transactional
    public void process(TransactionData transaction) {
        String eventId = transaction.getEventId();
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try {
                processWithOptimisticLocking(transaction);
                log.info("Transaction {} processed successfully", eventId);
                return;

            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries reached for transaction after optimistic lock conflicts");
                    throw new TransactionProcessingException(
                            "Failed to process transaction after " + MAX_RETRIES + " attempts", e);
                }

                log.warn("Optimistic lock conflict on attempt {} for transaction {}, retrying...", attempt, eventId);

                try {
                    Thread.sleep(RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TransactionProcessingException("Thread interrupted during retry", ie);
                }
            }
        }
    }

    private void processWithOptimisticLocking(TransactionData transaction) {
        Account sourceAccount = accountRepository.findById(
                        transaction.getSourceAccount().getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: "
                + transaction.getSourceAccount().getAccountId()));

        Account destinationAccount = accountRepository.findById(
                        transaction.getDestinationAccount().getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: "
                + transaction.getDestinationAccount().getAccountId()));

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

        Transaction debitTransaction = Transaction.builder()
                .accountId(transaction.getSourceAccount().getAccountId())
                .eventId(transaction.getEventId())
                .type(TransactionType.DEBIT)
                .amount(transaction.getSourceAccount().getAmount())
                .currency(transaction.getSourceAccount().getCurrency())
                .balanceAfter(sourceAccount.getBalance())
                .operationDate(transaction.getOperationDate())
                .processedAt(LocalDateTime.now())
                .status(TransactionStatus.APPLIED)
                .build();

        Transaction creditTransaction = Transaction.builder()
                .accountId(transaction.getDestinationAccount().getAccountId())
                .eventId(transaction.getEventId())
                .type(TransactionType.CREDIT)
                .amount(transaction.getDestinationAccount().getAmount())
                .currency(transaction.getDestinationAccount().getCurrency())
                .balanceAfter(destinationAccount.getBalance())
                .operationDate(transaction.getOperationDate())
                .processedAt(LocalDateTime.now())
                .status(TransactionStatus.APPLIED)
                .build();


        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        log.info("Transaction processed successfully: {} -> {}",
                sourceAccount.getId(),
                destinationAccount.getId());
    }

    private void validateTransactionCurrency(Account account, String currency) {
        if (!account.getCurrency().equals(currency)) {
            throw new InvalidCurrencyException("Currency mismatch for account " + account.getId() +
                    ". Expected: " + account.getCurrency() + ", Got: " + currency);
        }
    }
}
