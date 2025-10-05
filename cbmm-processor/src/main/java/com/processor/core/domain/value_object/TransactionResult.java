package com.processor.core.domain.value_object;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResult {
    private String eventId;
    private TransactionStatus status;
    private String message;
    private LocalDateTime processedAt;

    public enum TransactionStatus {
        SUCCESS,
        ALREADY_PROCESSED,
        ALREADY_PROCESSING,
        FAILED
    }

    public static TransactionResult success(String eventId) {
        return TransactionResult.builder()
                .eventId(eventId)
                .status(TransactionStatus.SUCCESS)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static TransactionResult alreadyProcessed(String eventId) {
        return TransactionResult.builder()
                .eventId(eventId)
                .status(TransactionStatus.ALREADY_PROCESSED)
                .message("Event already processed")
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static TransactionResult alreadyProcessing(String eventId) {
        return TransactionResult.builder()
                .eventId(eventId)
                .status(TransactionStatus.ALREADY_PROCESSING)
                .message("Event is being processed by another thread")
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static TransactionResult failed(String eventId, String errorMessage) {
        return TransactionResult.builder()
                .eventId(eventId)
                .status(TransactionStatus.FAILED)
                .message(errorMessage)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
