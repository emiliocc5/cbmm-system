package com.processor.application.service;

import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.exception.TransactionProcessingException;
import com.processor.core.domain.value_object.TransactionResult;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.IdempotencyChecker;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.infrastructure.config.TransactionConfig;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class CbmmTransactionApplicationService {

    private final TransactionConfig transactionConfig;
    private final ProcessCbmmTransactionUseCase useCase;
    private final IdempotencyChecker idempotencyChecker;

    public List<CompletableFuture<TransactionResult>> processTransactionsConcurrently(
            List<TransactionData> transactions) {

        return transactions.stream()
                .map(this::processTransactionAsync)
                .toList();
    }


    @Async("cbmmTransactionExecutor")
    public CompletableFuture<TransactionResult> processTransactionAsync(
            TransactionData transaction) {

        String eventId = transaction.getEventId();

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (idempotencyChecker.isProcessed(eventId)) {
                    log.info("Event {} already processed, skipping", eventId);
                    return TransactionResult.alreadyProcessed(eventId);
                }

                if (!idempotencyChecker.tryMarkAsProcessing(eventId)) {
                    log.warn("Event {} is already being processed by another thread", eventId);
                    return TransactionResult.alreadyProcessing(eventId);
                }

                processTransaction(transaction);

                idempotencyChecker.markAsProcessed(eventId);
                log.info("Event {} processed successfully", eventId);

                return TransactionResult.success(eventId);

            } catch (Exception e) {
                log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
                idempotencyChecker.markAsFailed(eventId, e.getMessage());
                return TransactionResult.failed(eventId, e.getMessage());
            }
        });
    }

    private void processTransaction(TransactionData transaction) {
        String eventId = transaction.getEventId();
        int attempt = 0;

        while (attempt < transactionConfig.getMaxAttempts()) {
            try {
                useCase.process(transaction);
                log.info("Transaction {} processed successfully on attempt {}", eventId, attempt + 1);
                return;

            } catch (OptimisticLockingFailureException | StaleObjectStateException | OptimisticLockException e) {
                attempt++;

                if (attempt >= transactionConfig.getMaxAttempts()) {
                    log.error("Max retries ({}) reached for transaction {} after optimistic lock conflicts",
                            transactionConfig.getMaxAttempts(), eventId);
                    throw new TransactionProcessingException(
                            String.format("Failed to process transaction %s after %d attempts due to concurrent modifications",
                                    eventId, transactionConfig.getMaxAttempts()), e);
                }

                long backoffDelay = calculateBackoffWithJitter(attempt);
                log.warn("Optimistic lock conflict on attempt {} for transaction {}, retrying after {}ms...",
                        attempt, eventId, backoffDelay);

                sleep(backoffDelay);

            } catch (InsufficientFundsException | InvalidCurrencyException | AccountNotFoundException be) {
                log.error("Business validation error processing transaction {}: {}", eventId, be.getMessage());
                throw be;

            } catch (Exception e) {
                log.error("Unexpected error processing transaction {}: {}", eventId, e.getMessage(), e);
                throw new TransactionProcessingException(
                        String.format("Unexpected error processing transaction %s", eventId), e);
            }
        }
    }

    //Temporal dispersion of threads
    private long calculateBackoffWithJitter(int attempt) {
        long exponentialDelay = transactionConfig.getBaseDelayMs() * (long) Math.pow(2, attempt - 1);
        long cappedDelay = Math.min(exponentialDelay, transactionConfig.getMaxDelayMs());


        double jitterFactor = 0.75 + (Math.random() * 0.5);
        long delayWithJitter = (long) (cappedDelay * jitterFactor);

        return Math.max(transactionConfig.getBaseDelayMs(), delayWithJitter);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransactionProcessingException("Thread interrupted during retry backoff", e);
        }
    }

    public TransactionResult processTransactionSync(TransactionData transaction) {
        String eventId = transaction.getEventId();

        if (idempotencyChecker.isProcessed(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return TransactionResult.alreadyProcessed(eventId);
        }

        if (!idempotencyChecker.tryMarkAsProcessing(eventId)) {
            log.warn("Event {} is already being processed", eventId);
            return TransactionResult.alreadyProcessing(eventId);
        }

        try {
            useCase.process(transaction);
            idempotencyChecker.markAsProcessed(eventId);
            return TransactionResult.success(eventId);

        } catch (Exception e) {
            idempotencyChecker.markAsFailed(eventId, e.getMessage());
            throw e;
        }
    }

    public List<TransactionResult> waitForAllTransactions(
            List<CompletableFuture<TransactionResult>> futures) {

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
