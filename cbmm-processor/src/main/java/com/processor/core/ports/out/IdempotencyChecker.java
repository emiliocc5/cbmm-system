package com.processor.core.ports.out;

public interface IdempotencyChecker {
    boolean checkEvent(String eventId);
    void markAsProcessing(String eventId);
    void markAsProcessed(String eventId);
    void release(String eventId);
}
