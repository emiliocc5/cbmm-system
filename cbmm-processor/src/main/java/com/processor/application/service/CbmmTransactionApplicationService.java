package com.processor.application.service;

import com.processor.core.domain.value_object.TransactionResult;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.IdempotencyChecker;
import com.processor.core.domain.value_object.TransactionData;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class CbmmTransactionApplicationService {
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

                useCase.process(transaction);

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

    @Transactional
    public TransactionResult processTransaction(TransactionData transaction) {
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
