package com.payflow.transaction.api;

import com.payflow.transaction.internal.util.TransactionDTO;

public interface TransactionFacade {
    TransactionDTO findTransactionById(Long id);

    void updateTransactionStatus(Long transactionId, String processed);
}
