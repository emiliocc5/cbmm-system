package com.processor.application.service;

import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.IdempotencyChecker;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.use_case.ProcessCbmmTransactionUseCaseImpl;
import com.processor.infrastructure.adapters.out.reddis.IdempotencyCheckerImpl;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CbmmTransactionApplicationService {
    private static final Logger log = LoggerFactory.getLogger(CbmmTransactionApplicationService.class);
    private final ProcessCbmmTransactionUseCase useCase;
    private final IdempotencyChecker idempotencyChecker;

    @Transactional
    public void processTransactions(List<TransactionData> transactions, String eventId) {
        if (idempotencyChecker.checkEvent(eventId)) {
            log.warn("Event is processing");
            //throw new AlreadyProcessingException();
        }
        idempotencyChecker.markAsProcessing(eventId);
        try {
            for (TransactionData tx : transactions) {
                useCase.process(tx);
            }
            idempotencyChecker.markAsProcessed(eventId);
        } catch (Exception e) {
            idempotencyChecker.release(eventId);
            throw e;
        }
    }
}
