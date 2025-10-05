package com.processor.core.ports.in;

import com.processor.core.domain.value_object.TransactionData;

public interface ProcessCbmmTransactionUseCase {
    void process(TransactionData transaction);
}
