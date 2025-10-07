package com.processor.core.domain.value_object;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferAccount {
    private String accountId;
    private String currency;
    private BigDecimal amount;
}
