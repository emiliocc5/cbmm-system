package com.processor.infrastructure.adapters.out.reddis;

import com.processor.core.domain.enums.ProcessingStatus;
import com.processor.core.ports.out.IdempotencyChecker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class IdempotencyCheckerImpl implements IdempotencyChecker {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "cbmm:event:";
    private static final String PROCESSING_SUFFIX = ":processing";

    //TODO take this to environment variables
    private static final long PROCESSING_TTL_SECONDS = 300; // 5 min
    private static final long SUCCESS_TTL_SECONDS = 86400; // 24 horas
    private static final long FAILED_TTL_SECONDS = 86400; // 24 horas

    @Override
    public boolean isProcessed(String eventId) {
        String key = buildKey(eventId);
        String value = stringRedisTemplate.opsForValue().get(key);
        return ProcessingStatus.SUCCESS.name().equals(value);
    }

    @Override
    public boolean tryMarkAsProcessing(String eventId) {
        String key = buildKey(eventId);
        String processingKey = key + PROCESSING_SUFFIX;

        try {
            if (isProcessed(eventId)) {
                log.info("Event {} already processed successfully", eventId);
                return false;
            }

            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(processingKey,
                            ProcessingStatus.PROCESSING.name(),
                            Duration.ofSeconds(PROCESSING_TTL_SECONDS));

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Event {} marked as processing", eventId);
                return true;
            } else {
                log.warn("Event {} is already being processed by another instance", eventId);
                return false;
            }

        } catch (Exception e) {
            log.error("Error trying to mark event {} as processing: {}",
                    eventId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void markAsProcessed(String eventId) {
        String key = buildKey(eventId);
        String processingKey = key + PROCESSING_SUFFIX;

        try {
            stringRedisTemplate.opsForValue()
                    .set(key,
                            ProcessingStatus.SUCCESS.name(),
                            Duration.ofSeconds(SUCCESS_TTL_SECONDS));

            stringRedisTemplate.delete(processingKey);

            log.info("Event {} marked as successfully processed", eventId);

        } catch (Exception e) {
            log.error("Error marking event {} as processed: {}",
                    eventId, e.getMessage(), e);
        }
    }

    @Override
    public void markAsFailed(String eventId, String errorMessage) {
        String key = buildKey(eventId);
        String processingKey = key + PROCESSING_SUFFIX;

        try {
            stringRedisTemplate.opsForValue()
                    .set(key,
                            ProcessingStatus.FAILED.name(),
                            Duration.ofSeconds(FAILED_TTL_SECONDS));

            stringRedisTemplate.delete(processingKey);

            log.error("Event {} marked as failed: {}", eventId, errorMessage);

        } catch (Exception e) {
            log.error("Error marking event {} as failed: {}",
                    eventId, e.getMessage(), e);
        }
    }

    @Override
    public void release(String eventId) {
        String processingKey = buildKey(eventId) + PROCESSING_SUFFIX;

        try {
            stringRedisTemplate.delete(processingKey);
            log.info("Released processing lock for event {}", eventId);
        } catch (Exception e) {
            log.error("Error releasing lock for event {}: {}",
                    eventId, e.getMessage(), e);
        }
    }

    private String buildKey(String eventId) {
        return KEY_PREFIX + eventId;
    }

}
