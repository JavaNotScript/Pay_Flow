package com.payflow.transaction.internal.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.domain.EventType;
import com.payflow.transaction.api.ExchangeAdapter;
import com.payflow.transaction.internal.domain.*;
import com.payflow.common.ex.TransactionException;
import com.payflow.outbox.OutboxEvent;
import com.payflow.outbox.OutboxRepository;
import com.payflow.outbox.StatusEnum;
import com.payflow.transaction.internal.repos.TransactionRepository;
import com.payflow.transaction.internal.util.ConversionResult;
import com.payflow.transaction.internal.util.DepositResponse;
import com.payflow.wallet.api.WalletFacade;
import com.payflow.wallet.internal.util.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ExchangeAdapter exchangeAdapter;
    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public DepositResponse requestDeposit(Long userId, BigDecimal amount,String idempotencyKey,String depositCurrency) {
        logger.info("userId={} , amount={} ,idempotencyKey={}, depositCurrency={} ",userId,amount,idempotencyKey,depositCurrency);

        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new TransactionException("idempotencyKey cannot be null or empty");
        }

        //amount check
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount must be greater than zero");
        }
        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        //prevents duplicate transactions
        if (existingTransaction.isPresent()) {
            return mapToResponse(existingTransaction.get());
        }

        WalletInfo walletInfo = walletFacade.getWalletByUserId(userId);

        CurrencyEnum currencyFrom;
        CurrencyEnum currencyTo;

        try {
            currencyFrom = CurrencyEnum.valueOf(depositCurrency.trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new TransactionException("Unsupported deposit currency, "+depositCurrency);
        }

        try {
            currencyTo = CurrencyEnum.valueOf(walletInfo.currency().trim().toUpperCase());
        }catch (IllegalArgumentException e){
            throw new TransactionException("Unsupported wallet currency, "+walletInfo.currency());
        }

        ConversionResult conversion = exchangeAdapter.convert(amount,currencyFrom,currencyTo);

        return persistDeposit(idempotencyKey,walletInfo,amount,currencyFrom,conversion,userId);
    }


    //refactor to a new bean to prevent it transactional from being skipped
    @Transactional
    protected DepositResponse persistDeposit(String idempotencyKey, WalletInfo walletInfo,BigDecimal amount,CurrencyEnum currencyFrom,ConversionResult conversion,Long userId){

        try {
            Transaction transaction = new Transaction();
            transaction.setWalletDestinationId(walletInfo.walletId());
            transaction.setSourceWalletId(null);
            transaction.setSourceAmount(amount);
            transaction.setSourceCurrency(currencyFrom.name());

            transaction.setDestinationCurrency(walletInfo.currency());
            transaction.setDestinationAmount(conversion.destinationAmount());

            transaction.setExchangeRate(conversion.rateUsed());

            transaction.setPaymentMethod(PaymentMethod.DEPOSIT);
            transaction.setTransactionDirection(TransactionDirection.CREDIT);
            transaction.setTransactionStatus(TransactionStatus.PENDING);
            transaction.setIdempotencyKey(idempotencyKey);
            //transaction.setExternalReference(); comes from provider -mpesa stripe


            Transaction savedTransaction = transactionRepository.save(transaction);

            JsonNode payload = objectMapper.valueToTree(Map.of("transactionId",savedTransaction.getTransactionId()));

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setStatus(StatusEnum.PENDING);
            outboxEvent.setUserId(userId);
            outboxEvent.setEventType(EventType.DEPOSIT_REQUEST);
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
