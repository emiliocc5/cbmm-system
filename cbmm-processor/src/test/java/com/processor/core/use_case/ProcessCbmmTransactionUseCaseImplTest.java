package com.processor.core.use_case;

import com.processor.core.domain.enums.TransactionType;
import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.model.Account;
import com.processor.core.domain.model.Transaction;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransferAccount;
import com.processor.core.ports.out.AccountRepository;
import com.processor.core.ports.out.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessCbmmTransactionUseCaseImplTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ProcessCbmmTransactionUseCaseImpl processCbmmTransactionUseCaseImpl;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private static final String SOURCE_ACCOUNT_ID = "sourceAccountId";
    private static final String DEST_ACCOUNT_ID = "destAccountId";
    private static final String SOURCE_VALID_CURRENCY = "USD";
    private static final String DEST_VALID_CURRENCY = "EUR";
    private static final BigDecimal INITIAL_SOURCE_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal INITIAL_DEST_BALANCE = new BigDecimal("50.00");
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("30.00");

    @Test
    @DisplayName("Should process transaction successfully when all validations pass")
    void testGivenValidTransaction_ThenProcessSuccess() {
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                TRANSFER_AMOUNT, TRANSFER_AMOUNT
        );

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
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                TRANSFER_AMOUNT, TRANSFER_AMOUNT
        );

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, insufficientBalance, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, DEST_VALID_CURRENCY);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Source account has not enough balance");

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when source account currency does not match")
    void testGivenInvalidSourceCurrency_ThenThrowException() {
        String invalidCurrency = "BRL";
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                TRANSFER_AMOUNT, TRANSFER_AMOUNT
        );

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, invalidCurrency);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency not match for account");

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when destination account currency does not match")
    void testGivenInvalidDestinationCurrency_ThenThrowException() {
        String invalidCurrency = "GBP";
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                TRANSFER_AMOUNT, TRANSFER_AMOUNT
        );

        Account sourceAccount = createAccount(SOURCE_ACCOUNT_ID, INITIAL_SOURCE_BALANCE, SOURCE_VALID_CURRENCY);
        Account destAccount = createAccount(DEST_ACCOUNT_ID, INITIAL_DEST_BALANCE, invalidCurrency);

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(DEST_ACCOUNT_ID)).thenReturn(Optional.of(destAccount));

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("Currency not match for account");

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
        TransactionData transaction = createTransactionData(
                SOURCE_ACCOUNT_ID, DEST_ACCOUNT_ID, SOURCE_VALID_CURRENCY, DEST_VALID_CURRENCY,
                TRANSFER_AMOUNT, TRANSFER_AMOUNT
        );

        when(accountRepository.findById(SOURCE_ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processCbmmTransactionUseCaseImpl.process(transaction))
                .isInstanceOf(AccountNotFoundException.class);
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

    private TransactionData createTransactionData(String sourceId, String destId,
                                                  String sourceCurrency, String destCurrency,
                                                  BigDecimal sourceAmount, BigDecimal destAmount) {
        TransferAccount source = new TransferAccount(sourceId, sourceCurrency, sourceAmount);
        TransferAccount dest = new TransferAccount(destId, destCurrency, destAmount);
        return new TransactionData(source, dest, new Date());
    }

    private Account createAccount(String accountId, BigDecimal balance, String currency) {
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(balance);
        account.setCurrency(currency);
        return account;
    }
}
