package com.processor.infrastructure.adapters.out.reddis;

import com.processor.core.ports.out.IdempotencyChecker;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyCheckerImpl implements IdempotencyChecker {
    @Override
    public boolean checkEvent(String eventId) {
        return false;
    }

    @Override
    public void markAsProcessing(String eventId) {

    }

    @Override
    public void markAsProcessed(String eventId) {

    }

    @Override
    public void release(String eventId) {

    }
}
