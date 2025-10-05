package com.processor.core.domain.model;

import com.processor.core.domain.enums.TransactionType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
public class Transaction {
    @Id
    private String id;
    private String accountId;
    private BigDecimal amount;
    private TransactionType type;

    protected Transaction() {}

    public Transaction(String accountId, BigDecimal amount, TransactionType type) {
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
    }

}
