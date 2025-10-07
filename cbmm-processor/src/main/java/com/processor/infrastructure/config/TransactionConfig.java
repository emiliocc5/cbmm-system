package com.processor.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transaction.retry")
@Getter
@Setter
public class TransactionConfig {
    private Integer maxAttempts;
    private Long baseDelayMs;
    private Long maxDelayMs;
}
