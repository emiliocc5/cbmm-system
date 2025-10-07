package com.processor.core.domain.exception;

import org.springframework.dao.OptimisticLockingFailureException;

public class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message, OptimisticLockingFailureException e) {
        super(message, e);
    }
  public TransactionProcessingException(String message, InterruptedException e) {
    super(message, e);
  }
}
