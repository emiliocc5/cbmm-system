package com.processor.infrastructure.adapters.in.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.processor.application.service.CbmmTransactionApplicationService;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransactionResult;
import com.processor.core.domain.value_object.TransferAccount;
import com.processor.infrastructure.adapters.in.http.dto.BatchProcessingResponse;
import com.processor.infrastructure.adapters.in.http.dto.EventDTO;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final ObjectMapper objectMapper;

    @PostMapping("/process-batch")
    public ResponseEntity<BatchProcessingResponse> processBatch(
            @RequestBody List<EventDTO> eventDTOS) {

        List<TransactionData> transactions = eventDTOS.stream()
                .map(this::mapFromEventDTO)
                .toList();

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

    @PostMapping(value = "/process-batch-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchProcessingResponse> processBatchFile(
            @RequestParam("file") MultipartFile file) {
        try {
            List<EventDTO> eventDTOS = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<>() {}
            );

            return processBatch(eventDTOS);
        } catch (IOException e) {
            log.error("Error reading JSON File", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/process-single")
    public ResponseEntity<TransactionResult> processSingle(
            @RequestBody EventDTO event) {
        TransactionResult result = applicationService.processTransactionSync(mapFromEventDTO(event));
        return ResponseEntity.ok(result);
    }

    private TransactionData mapFromEventDTO(EventDTO event) {
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

        return transaction;
    }
}
