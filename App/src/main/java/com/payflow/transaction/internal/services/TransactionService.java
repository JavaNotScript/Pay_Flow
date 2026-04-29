package com.payflow.transaction.internal.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.transaction.api.ExchangeAdapter;
import com.payflow.transaction.internal.domain.*;
import com.payflow.common.ex.TransactionException;
import com.payflow.outbox.OutboxEvent;
import com.payflow.outbox.OutboxRepository;
import com.payflow.outbox.StatusEnum;
import com.payflow.transaction.internal.repos.ExchangeRepository;
import com.payflow.transaction.internal.repos.TransactionRepository;
import com.payflow.transaction.internal.util.DepositResponse;
import com.payflow.wallet.api.WalletFacade;
import com.payflow.wallet.internal.util.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final WalletFacade walletFacade;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeAdapter exchangeAdapter;

    @Transactional
    public DepositResponse requestDeposit(Long userId, BigDecimal amount,String idempotencyKey,String depositCurrency) {
        //amount check
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount must be greater than zero");
        }

        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new TransactionException("idempotencyKey cannot be null or empty");
        }

        WalletInfo walletInfo = walletFacade.getWalletByUserId(userId);

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            return mapToResponse(existingTransaction.get());
        }

        CurrencyEnum currencyFrom = CurrencyEnum.valueOf(depositCurrency);
        CurrencyEnum currencyTo = CurrencyEnum.valueOf(walletInfo.currency());

        BigDecimal depositAmount = exchangeAdapter.convertToUSD(amount,currencyFrom,currencyTo);

        try {
            Transaction transaction = new Transaction();
            transaction.setWalletDestinationId(walletInfo.walletId());
            transaction.setSourceWalletId(null);
            transaction.setDestinationCurrency(walletInfo.currency());
            transaction.setSourceAmount(depositAmount);
            transaction.setPaymentMethod(PaymentMethod.DEPOSIT);
            transaction.setTransactionDirection(TransactionDirection.CREDIT);
            transaction.setTransactionStatus(TransactionStatus.PENDING);
            transaction.setIdempotencyKey(idempotencyKey);
            //transaction.setExternalReference(); comes from provider -mpesa stripe


            Transaction savedTransaction = transactionRepository.save(transaction);

            JsonNode payload = objectMapper.valueToTree(Map.of("transactionId",transaction.getTransactionId()));

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setStatus(StatusEnum.PENDING);
            outboxEvent.setUserId(userId);
            outboxEvent.setEvent_type("DEPOSIT_REQUEST");
            outboxEvent.setPayload(payload);
            outboxEvent.setRetryCount(0);

            outboxRepository.save(outboxEvent);

            return mapToResponse(savedTransaction);
        }catch (DataIntegrityViolationException e) {
            Transaction existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

            return mapToResponse(existingTx);
        }
    }

    private DepositResponse mapToResponse(Transaction transaction) {
        return new DepositResponse(
                transaction.getTransactionId(),
                transaction.getWalletDestinationId(),
                transaction.getTransactionStatus().name(),
                transaction.getDestinationAmount(),
                transaction.getDestinationCurrency()

        );
    }
}
