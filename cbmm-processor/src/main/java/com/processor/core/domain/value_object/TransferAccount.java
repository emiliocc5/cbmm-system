package com.processor.core.domain.value_object;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TransferAccount {
    private String accountId;
    private String currency;
    private BigDecimal amount;
}
