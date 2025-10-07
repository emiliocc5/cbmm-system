package com.processor.core.domain.model;

import com.processor.core.domain.enums.TransactionStatus;
import com.processor.core.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "transactions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Transaction {
    @Id
    private String id;
    private String accountId;
    private String eventId;
    private String currency;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private LocalDateTime operationDate;
    private LocalDateTime processedAt;

}
