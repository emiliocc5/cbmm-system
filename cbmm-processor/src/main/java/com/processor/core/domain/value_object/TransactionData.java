package com.processor.core.domain.value_object;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionData {
    private String eventId;
    private TransferAccount sourceAccount;
    private TransferAccount destinationAccount;
    private LocalDateTime operationDate;
}
