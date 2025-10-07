package com.processor.core.domain.value_object;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class TransactionData {
    private TransferAccount sourceAccount;
    private TransferAccount destinationAccount;
    private Date operationDate;
}
