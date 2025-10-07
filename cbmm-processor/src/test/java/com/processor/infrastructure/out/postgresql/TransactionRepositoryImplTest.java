package com.processor.infrastructure.out.postgresql;

import com.processor.core.domain.model.Transaction;
import com.processor.infrastructure.adapters.out.postgresql.PostgresTransactionRepository;
import com.processor.infrastructure.adapters.out.postgresql.TransactionRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionRepositoryImplTest {
    @Mock
    PostgresTransactionRepository postgresTransactionRepository;

    @InjectMocks
    TransactionRepositoryImpl transactionRepository;

    @Test
    @DisplayName("Should save transaction successfully")
    void testGivenValidTransaction_thenSaveTransactionSuccessfully() {
        Transaction transaction = new Transaction();

        when(postgresTransactionRepository.save(transaction)).thenReturn(transaction);

        transactionRepository.save(transaction);

        verify(postgresTransactionRepository).save(transaction);
    }

}
