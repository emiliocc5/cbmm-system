package com.processor.core.use_case;

import com.processor.core.domain.enums.TransactionType;
import com.processor.core.domain.exception.AccountNotFoundException;
import com.processor.core.domain.exception.InsufficientFundsException;
import com.processor.core.domain.exception.InvalidCurrencyException;
import com.processor.core.domain.model.Account;
import com.processor.core.domain.model.Transaction;
import com.processor.core.domain.value_object.TransactionData;
import com.processor.core.ports.in.ProcessCbmmTransactionUseCase;
import com.processor.core.ports.out.AccountRepository;
import com.processor.core.ports.out.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
@Slf4j
public class ProcessCbmmTransactionUseCaseImpl implements ProcessCbmmTransactionUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;


    @Override
    public void process(TransactionData transaction) {
        Account sourceAccount = accountRepository.findById(transaction.getSourceAccount().getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found"));
        ;
        validateTransactionCurrency(sourceAccount, transaction.getSourceAccount().getCurrency());

        Account destinationAccount = accountRepository.findById(transaction.getDestinationAccount().getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

        validateTransactionCurrency(destinationAccount, transaction.getDestinationAccount().getCurrency());

        if (sourceAccount.getBalance().compareTo(transaction.getSourceAccount().getAmount()) < 0) {
            throw new InsufficientFundsException("Source account has not enough balance");
        }

        sourceAccount.debit(transaction.getSourceAccount().getAmount());
        destinationAccount.credit(transaction.getDestinationAccount().getAmount());

        Transaction debitTransaction = new Transaction(transaction.getSourceAccount().getAccountId(),
                transaction.getSourceAccount().getAmount(),
                TransactionType.DEBIT);

        Transaction creditTransaction = new Transaction(transaction.getDestinationAccount().getAccountId(),
                transaction.getDestinationAccount().getAmount(),
                TransactionType.CREDIT);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);
    }

    private void validateTransactionCurrency(Account account, String currency) {
        if (!account.getCurrency().equals(currency)) {
            throw new InvalidCurrencyException("Currency not match for account");
        }
    }
}
