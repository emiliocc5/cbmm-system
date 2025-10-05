package com.processor.infrastructure.adapters.in.http.dto;

import com.processor.core.domain.value_object.TransactionResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchProcessingResponse {
    private Integer totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;
    private List<TransactionResult> results;
}
