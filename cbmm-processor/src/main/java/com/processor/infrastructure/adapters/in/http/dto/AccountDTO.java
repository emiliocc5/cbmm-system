package com.processor.infrastructure.adapters.in.http.dto;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class AccountDTO {
    String account_id;
    String currency;
    BigDecimal amount;
}
