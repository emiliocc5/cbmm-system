package com.processor.core.ports.out;

public interface IdempotencyChecker {
    boolean isProcessed(String eventId);
    boolean tryMarkAsProcessing(String eventId);
    void markAsProcessed(String eventId);
    void markAsFailed(String eventId, String errorMessage);
    void release(String eventId);
}
