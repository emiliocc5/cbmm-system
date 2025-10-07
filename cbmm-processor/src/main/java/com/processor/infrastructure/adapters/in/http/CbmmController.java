package com.processor.infrastructure.adapters.in.http;

import com.processor.application.service.CbmmTransactionApplicationService;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransactionResult;
import com.processor.core.domain.value_object.TransferAccount;
import com.processor.infrastructure.adapters.in.http.dto.BatchProcessingResponse;
import com.processor.infrastructure.adapters.in.http.dto.EventDTO;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/cbmm")
@AllArgsConstructor
@Slf4j
public class CbmmController {
    private final CbmmTransactionApplicationService applicationService;

    @PostMapping("/process-batch")
    public ResponseEntity<BatchProcessingResponse> processBatch(
            @RequestBody List<TransactionData> transactions) {

        List<CompletableFuture<TransactionResult>> futures =
                applicationService.processTransactionsConcurrently(transactions);

        List<TransactionResult> results =
                applicationService.waitForAllTransactions(futures);

        long successCount = results.stream()
                .filter(r -> r.getStatus() == TransactionResult.TransactionStatus.SUCCESS)
                .count();

        long failedCount = results.stream()
                .filter(r -> r.getStatus() == TransactionResult.TransactionStatus.FAILED)
                .count();

        BatchProcessingResponse response = BatchProcessingResponse.builder()
                .totalTransactions(transactions.size())
                .successfulTransactions(successCount)
                .failedTransactions(failedCount)
                .results(results)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-single")
    public ResponseEntity<TransactionResult> processSingle(
            @RequestBody EventDTO event) {
        log.info("Received event {}", event);
        TransactionData transaction = new TransactionData();
        transaction.setEventId(event.getEvent_id());
        LocalDateTime operationDate = ZonedDateTime
                .parse(event.getOperation_date(), DateTimeFormatter.ISO_ZONED_DATE_TIME)
                .toLocalDateTime();
        transaction.setOperationDate(operationDate);

        TransferAccount sourceAccount = new TransferAccount();
        sourceAccount.setAccountId(event.getOrigin().getAccount_id());
        sourceAccount.setAmount(event.getOrigin().getAmount());
        sourceAccount.setCurrency(event.getOrigin().getCurrency());

        TransferAccount destinationAccount = new TransferAccount();
        destinationAccount.setAccountId(event.getDestination().getAccount_id());
        destinationAccount.setAmount(event.getDestination().getAmount());
        destinationAccount.setCurrency(event.getDestination().getCurrency());

        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);

        TransactionResult result = applicationService.processTransaction(transaction);
        return ResponseEntity.ok(result);
    }
}
