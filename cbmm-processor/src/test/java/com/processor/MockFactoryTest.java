package com.processor;

import com.processor.core.domain.model.Account;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.domain.value_object.TransferAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MockFactoryTest {
    protected static final String EVENT_ID = "eventId";
    protected static final String SOURCE_ACCOUNT_ID = "sourceAccountId";
    protected static final String DEST_ACCOUNT_ID = "destAccountId";
    protected static final String SOURCE_VALID_CURRENCY = "USD";
    protected static final String DEST_VALID_CURRENCY = "EUR";
    protected static final BigDecimal INITIAL_SOURCE_BALANCE = new BigDecimal("100.00");
    protected static final BigDecimal INITIAL_DEST_BALANCE = new BigDecimal("50.00");
    protected static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("30.00");

    protected TransactionData createTransactionData() {
        return createTransactionData(EVENT_ID);
    }

    protected TransactionData createTransactionData(String eventId) {
        TransferAccount source = new TransferAccount(
                SOURCE_ACCOUNT_ID, SOURCE_VALID_CURRENCY, TRANSFER_AMOUNT);
        TransferAccount dest = new TransferAccount(
                DEST_ACCOUNT_ID, DEST_VALID_CURRENCY, TRANSFER_AMOUNT);
        return new TransactionData(eventId, source, dest, LocalDateTime.now());
    }

    protected TransactionData createTransactionData(String sourceId, String destId,
                                                  String sourceCurrency, String destCurrency,
                                                  BigDecimal sourceAmount, BigDecimal destAmount) {
        TransferAccount source = new TransferAccount(sourceId, sourceCurrency, sourceAmount);
        TransferAccount dest = new TransferAccount(destId, destCurrency, destAmount);
        return new TransactionData(EVENT_ID, source, dest, LocalDateTime.now());
    }

    protected Account createAccount(String accountId, BigDecimal balance, String currency) {
        return new Account(accountId, balance, currency, 1L, LocalDateTime.now(), LocalDateTime.now());
    }
}
