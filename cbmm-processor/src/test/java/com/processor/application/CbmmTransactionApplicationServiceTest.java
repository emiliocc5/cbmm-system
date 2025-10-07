package com.processor.application;

import com.processor.MockFactoryTest;
import com.processor.application.service.CbmmTransactionApplicationService;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransactionResult;
import com.processor.core.domain.value_object.TransferAccount;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.IdempotencyChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CbmmTransactionApplicationServiceTest extends MockFactoryTest {
    @Mock
    private ProcessCbmmTransactionUseCase processCbmmTransactionUseCase;

    @Mock
    private IdempotencyChecker idempotencyChecker;

    @InjectMocks
    private CbmmTransactionApplicationService cbmmTransactionApplicationService;

    @Test
    @DisplayName("Should process transaction async successfully")
    void testProcessTransactionAsync_Success() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(true);
        doNothing().when(processCbmmTransactionUseCase).process(transaction);
        doNothing().when(idempotencyChecker).markAsProcessed(EVENT_ID);

        CompletableFuture<TransactionResult> resultPromise =
                cbmmTransactionApplicationService.processTransactionAsync(transaction);
        TransactionResult result = resultPromise.join();

        assertEquals(TransactionResult.TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker).tryMarkAsProcessing(EVENT_ID);
        verify(processCbmmTransactionUseCase).process(transaction);
        verify(idempotencyChecker).markAsProcessed(EVENT_ID);
        verify(idempotencyChecker, never()).markAsFailed(anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip already processed transaction async")
    void testProcessTransactionAsync_AlreadyProcessed() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(true);

        CompletableFuture<TransactionResult> resultPromise =
                cbmmTransactionApplicationService.processTransactionAsync(transaction);
        TransactionResult result = resultPromise.join();

        assertEquals(TransactionResult.TransactionStatus.ALREADY_PROCESSED, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker, never()).tryMarkAsProcessing(anyString());
        verify(processCbmmTransactionUseCase, never()).process(any());
        verify(idempotencyChecker, never()).markAsProcessed(anyString());
    }

    @Test
    @DisplayName("Should handle transaction already being processed async")
    void testProcessTransactionAsync_AlreadyProcessing() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(false);

        CompletableFuture<TransactionResult> resultPromise =
                cbmmTransactionApplicationService.processTransactionAsync(transaction);
        TransactionResult result = resultPromise.join();

        assertEquals(TransactionResult.TransactionStatus.ALREADY_PROCESSING, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker).tryMarkAsProcessing(EVENT_ID);
        verify(processCbmmTransactionUseCase, never()).process(any());
        verify(idempotencyChecker, never()).markAsProcessed(anyString());
    }

    @Test
    @DisplayName("Should handle exception during async processing")
    void testProcessTransactionAsync_Exception() {
        TransactionData transaction = createTransactionData();
        String errorMessage = "Database connection failed";
        RuntimeException exception = new RuntimeException(errorMessage);

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(true);
        doThrow(exception).when(processCbmmTransactionUseCase).process(transaction);
        doNothing().when(idempotencyChecker).markAsFailed(EVENT_ID, errorMessage);

        CompletableFuture<TransactionResult> resultPromise =
                cbmmTransactionApplicationService.processTransactionAsync(transaction);
        TransactionResult result = resultPromise.join();

        assertEquals(TransactionResult.TransactionStatus.FAILED, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).markAsFailed(EVENT_ID, errorMessage);
        verify(idempotencyChecker, never()).markAsProcessed(anyString());
    }

    @Test
    @DisplayName("Should process transaction synchronously successfully")
    void testProcessTransaction_Success() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(true);
        doNothing().when(processCbmmTransactionUseCase).process(transaction);
        doNothing().when(idempotencyChecker).markAsProcessed(EVENT_ID);

        TransactionResult result = cbmmTransactionApplicationService.processTransaction(transaction);

        assertEquals(TransactionResult.TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker).tryMarkAsProcessing(EVENT_ID);
        verify(processCbmmTransactionUseCase).process(transaction);
        verify(idempotencyChecker).markAsProcessed(EVENT_ID);
    }

    @Test
    @DisplayName("Should skip already processed transaction synchronously")
    void testProcessTransaction_AlreadyProcessed() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(true);

        TransactionResult result = cbmmTransactionApplicationService.processTransaction(transaction);

        assertEquals(TransactionResult.TransactionStatus.ALREADY_PROCESSED, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker, never()).tryMarkAsProcessing(anyString());
        verify(processCbmmTransactionUseCase, never()).process(any());
    }

    @Test
    @DisplayName("Should handle transaction already being processed synchronously")
    void testProcessTransaction_AlreadyProcessing() {
        TransactionData transaction = createTransactionData();

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(false);

        TransactionResult result = cbmmTransactionApplicationService.processTransaction(transaction);

        assertEquals(TransactionResult.TransactionStatus.ALREADY_PROCESSING, result.getStatus());
        assertEquals(EVENT_ID, result.getEventId());

        verify(idempotencyChecker).isProcessed(EVENT_ID);
        verify(idempotencyChecker).tryMarkAsProcessing(EVENT_ID);
        verify(processCbmmTransactionUseCase, never()).process(any());
    }

    @Test
    @DisplayName("Should handle exception during synchronous processing and rethrow")
    void testProcessTransaction_Exception() {
        TransactionData transaction = createTransactionData();
        String errorMessage = "Database connection failed";
        RuntimeException exception = new RuntimeException(errorMessage);

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(true);
        doThrow(exception).when(processCbmmTransactionUseCase).process(transaction);
        doNothing().when(idempotencyChecker).markAsFailed(EVENT_ID, errorMessage);

        assertThrows(RuntimeException.class, () ->
                cbmmTransactionApplicationService.processTransaction(transaction));

        verify(idempotencyChecker).markAsFailed(EVENT_ID, errorMessage);
        verify(idempotencyChecker, never()).markAsProcessed(anyString());
    }

    @Test
    @DisplayName("Should process multiple transactions concurrently")
    void testProcessTransactionsConcurrently_MultipleTransactions() {
        List<TransactionData> transactions = List.of(
                createTransactionData("event1"),
                createTransactionData("event2"),
                createTransactionData("event3")
        );

        when(idempotencyChecker.isProcessed(anyString())).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(anyString())).thenReturn(true);
        doNothing().when(processCbmmTransactionUseCase).process(any());
        doNothing().when(idempotencyChecker).markAsProcessed(anyString());

        List<CompletableFuture<TransactionResult>> futures =
                cbmmTransactionApplicationService.processTransactionsConcurrently(transactions);

        assertEquals(3, futures.size());

        List<TransactionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        assertEquals(3, results.size());
        results.forEach(result ->
                assertEquals(TransactionResult.TransactionStatus.SUCCESS, result.getStatus()));

        verify(processCbmmTransactionUseCase, times(3)).process(any());
    }

    @Test
    @DisplayName("Should handle empty transaction list")
    void testProcessTransactionsConcurrently_EmptyList() {
        List<TransactionData> transactions = List.of();

        List<CompletableFuture<TransactionResult>> futures =
                cbmmTransactionApplicationService.processTransactionsConcurrently(transactions);

        assertTrue(futures.isEmpty());
        verify(processCbmmTransactionUseCase, never()).process(any());
    }

    @Test
    @DisplayName("Should handle single transaction")
    void testProcessTransactionsConcurrently_SingleTransaction() {
        List<TransactionData> transactions = List.of(createTransactionData());

        when(idempotencyChecker.isProcessed(EVENT_ID)).thenReturn(false);
        when(idempotencyChecker.tryMarkAsProcessing(EVENT_ID)).thenReturn(true);
        doNothing().when(processCbmmTransactionUseCase).process(any());
        doNothing().when(idempotencyChecker).markAsProcessed(EVENT_ID);

        List<CompletableFuture<TransactionResult>> futures =
                cbmmTransactionApplicationService.processTransactionsConcurrently(transactions);

        assertEquals(1, futures.size());
        TransactionResult result = futures.get(0).join();
        assertEquals(TransactionResult.TransactionStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should wait for all transactions to complete successfully")
    void testWaitForAllTransactions_Success() {
        List<CompletableFuture<TransactionResult>> futures = List.of(
                CompletableFuture.completedFuture(TransactionResult.success("event1")),
                CompletableFuture.completedFuture(TransactionResult.success("event2")),
                CompletableFuture.completedFuture(TransactionResult.success("event3"))
        );

        List<TransactionResult> results =
                cbmmTransactionApplicationService.waitForAllTransactions(futures);

        assertEquals(3, results.size());
        results.forEach(result ->
                assertEquals(TransactionResult.TransactionStatus.SUCCESS, result.getStatus()));
    }

    @Test
    @DisplayName("Should wait for all transactions with mixed results")
    void testWaitForAllTransactions_MixedResults() {
        List<CompletableFuture<TransactionResult>> futures = List.of(
                CompletableFuture.completedFuture(TransactionResult.success("event1")),
                CompletableFuture.completedFuture(TransactionResult.alreadyProcessed("event2")),
                CompletableFuture.completedFuture(TransactionResult.failed("event3", "Error"))
        );

        List<TransactionResult> results =
                cbmmTransactionApplicationService.waitForAllTransactions(futures);

        assertEquals(3, results.size());
        assertEquals(TransactionResult.TransactionStatus.SUCCESS, results.get(0).getStatus());
        assertEquals(TransactionResult.TransactionStatus.ALREADY_PROCESSED, results.get(1).getStatus());
        assertEquals(TransactionResult.TransactionStatus.FAILED, results.get(2).getStatus());
    }

    @Test
    @DisplayName("Should wait for empty list of futures")
    void testWaitForAllTransactions_EmptyList() {
        List<CompletableFuture<TransactionResult>> futures = List.of();

        List<TransactionResult> results =
                cbmmTransactionApplicationService.waitForAllTransactions(futures);

        assertTrue(results.isEmpty());
    }
}
