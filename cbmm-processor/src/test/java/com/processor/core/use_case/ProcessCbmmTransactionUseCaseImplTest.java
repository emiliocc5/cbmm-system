package com.processor.core.use_case;

import com.processor.MockFactoryTest;
import com.processor.core.domain.enums.TransactionStatus;
import com.processor.core.domain.enums.TransactionType;
import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.model.Account;
import com.processor.core.domain.model.Transaction;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.ports.out.AccountRepository;
import com.processor.core.ports.out.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessCbmmTransactionUseCaseImplTest extends MockFactoryTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProcessCbmmTransactionUseCaseImpl processCbmmTransactionUseCaseImpl;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    @DisplayName("Should process transaction successfully when all validations pass")
    void testGivenValidTransaction_ThenProcessSuccess() {
        TransactionData transaction = createTransactionData();

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        processCbmmTransactionUseCaseImpl.process(transaction);

        verify(accountRepository, times(2)).findById(anyString());
        verify(accountRepository, times(2)).save(accountCaptor.capture());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        List<Account> savedAccounts = accountCaptor.getAllValues();
        Account savedSourceAccount = savedAccounts.stream()
                .filter(acc -> acc.getId().equals(SOURCE_ACCOUNT_ID))
                .findFirst()
                .orElseThrow();
        Account savedDestAccount = savedAccounts.stream()
                .filter(acc -> acc.getId().equals(DEST_ACCOUNT_ID))
                .findFirst()
                .orElseThrow();

        assertThat(savedSourceAccount.getBalance())
                .isEqualByComparingTo(INITIAL_SOURCE_BALANCE.subtract(TRANSFER_AMOUNT));
        assertThat(savedDestAccount.getBalance())
                .isEqualByComparingTo(INITIAL_DEST_BALANCE.add(TRANSFER_AMOUNT));

        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(2, savedTransactions.size());

        Transaction debitTx = savedTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();
        Transaction creditTx = savedTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.CREDIT)
                .findFirst()
                .orElseThrow();

        assertThat(debitTx.getAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(debitTx.getAmount()).isEqualByComparingTo(TRANSFER_AMOUNT);
        assertThat(creditTx.getAccountId()).isEqualTo(DEST_ACCOUNT_ID);
        assertThat(creditTx.getAmount()).isEqualByComparingTo(TRANSFER_AMOUNT);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when source account has insufficient balance")
    void testGivenInsufficientBalance_ThenThrowException() {
        BigDecimal insufficientBalance = new BigDecimal("10.00");
        TransactionData transaction = createTransactionData();

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, insufficientBalance, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Source account has insufficient balance for event " + EVENT_ID);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when source account currency does not match")
    void testGivenInvalidSourceCurrency_ThenThrowException() {
        String invalidCurrency = "BRL";
        TransactionData transaction = createTransactionData();

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, invalidCurrency);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency mismatch for account " + sourceAccount.getId() + ". Expected: " +
                        sourceAccount.getCurrency()+ ", Got: " +  transaction.getSourceAccount().getCurrency());

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when destination account currency does not match")
    void testGivenInvalidDestinationCurrency_ThenThrowException() {
        String invalidCurrency = "GBP";
        TransactionData transaction = createTransactionData();

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, invalidCurrency);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency mismatch for account " + destAccount.getId() + ". Expected: " +
                        destAccount.getCurrency()+ ", Got: " +  transaction.getDestinationAccount().getCurrency());

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process transaction with exact balance")
    void testGivenExactBalance_ThenProcessSuccess() {
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                INITIAL_SOURCE_BALANCE, INITIAL_SOURCE_BALANCE
        );

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        processCbmmTransactionUseCaseImpl.process(transaction);

        verify(accountRepository, times(2)).save(accountCaptor.capture());

        Account savedSourceAccount = accountCaptor.getAllValues().stream()
                .filter(acc -> acc.getId().equals(SOURCE_ACCOUNT_ID))
                .findFirst()
                .orElseThrow();

        assertThat(savedSourceAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when source account is not found")
    void testGivenNonExistentSourceAccount_ThenThrowException() {
        TransactionData transaction = createTransactionData();

        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found: " + SOURCE_ACCOUNT_ID);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when destination account is not found")
    void testGivenNonExistentDestinationAccount_ThenThrowException() {
        TransactionData transaction = createTransactionData();

        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account not found: " + DEST_ACCOUNT_ID);

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle decimal amounts correctly")
    void testGivenDecimalAmounts_ThenProcessCorrectly() {
        BigDecimal decimalAmount = new BigDecimal("25.75");
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                decimalAmount, decimalAmount
        );

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        processCbmmTransactionUseCaseImpl.process(transaction);

        verify(accountRepository, times(2)).save(accountCaptor.capture());

        Account savedSourceAccount = accountCaptor.getAllValues().stream()
                .filter(acc -> acc.getId().equals(SOURCE_ACCOUNT_ID))
                .findFirst()
                .orElseThrow();

        assertThat(savedSourceAccount.getBalance())
                .isEqualByComparingTo(INITIAL_SOURCE_BALANCE.subtract(decimalAmount));
    }

    @Test
    @DisplayName("Should save transactions with correct status and timestamps")
    void testGivenValidTransaction_ThenSaveWithCorrectMetadata() {
        TransactionData transaction = createTransactionData();

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        processCbmmTransactionUseCaseImpl.process(transaction);

        verify(transactionRepository, times(2)).save(transactionCaptor.capture());

        List<Transaction> savedTransactions = getTransactions(transaction);

        Transaction debitTx = savedTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow();

        assertThat(debitTx.getBalanceAfter())
                .isEqualByComparingTo(INITIAL_SOURCE_BALANCE.subtract(TRANSFER_AMOUNT));

        Transaction creditTx = savedTransactions.stream()
                .filter(tx -> tx.getType() == TransactionType.CREDIT)
                .findFirst()
                .orElseThrow();

        assertThat(creditTx.getBalanceAfter())
                .isEqualByComparingTo(INITIAL_DEST_BALANCE.add(TRANSFER_AMOUNT));
    }

    private List<Transaction> getTransactions(TransactionData transaction) {
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();

        savedTransactions.forEach(tx -> {
            assertThat(tx.getEventId()).isEqualTo(EVENT_ID);
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.APPLIED);
            assertThat(tx.getProcessedAt()).isNotNull();
            assertThat(tx.getOperationDate()).isEqualTo(transaction.getOperationDate());
            assertThat(tx.getCurrency()).isIn(SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY);
        });
        return savedTransactions;
    }

}

