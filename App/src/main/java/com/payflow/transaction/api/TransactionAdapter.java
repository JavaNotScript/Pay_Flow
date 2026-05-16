package com.payflow.transaction.api;

import com.payflow.common.ex.TransactionException;
import com.payflow.transaction.internal.domain.Transaction;
import com.payflow.transaction.internal.domain.TransactionStatus;
import com.payflow.transaction.internal.repos.TransactionRepository;
import com.payflow.transaction.internal.util.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionFacade {
    private final TransactionRepository transactionRepository;

    @Override
    public TransactionDTO findTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new TransactionException("Transaction not found"));

        return new TransactionDTO(
                transaction.getWalletDestinationId(),
                transaction.getSourceWalletId(),
                transaction.getDestinationCurrency(),
                transaction.getDestinationAmount(),
                transaction.getSourceAmount()
        );
    }

    @Override
    public void updateTransactionStatus(Long transactionId, String tStatus) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        TransactionStatus transactionStatus = TransactionStatus.valueOf(tStatus.trim().toUpperCase());
        transaction.setTransactionStatus(transactionStatus);

        transactionRepository.save(transaction);
    }
}
