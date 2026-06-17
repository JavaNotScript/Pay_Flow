package com.payflow.transaction.internal.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.domain.EventType;
import com.payflow.common.ex.InsufficientFundsException;
import com.payflow.common.ex.TransactionException;
import com.payflow.outbox.OutboxEvent;
import com.payflow.outbox.OutboxRepository;
import com.payflow.outbox.StatusEnum;
import com.payflow.transaction.api.ExchangeAdapter;
import com.payflow.transaction.internal.domain.*;
import com.payflow.transaction.internal.repos.TransactionRepository;
import com.payflow.transaction.internal.util.depositRelated.DepositResponse;
import com.payflow.transaction.internal.util.exchange.ConversionResult;
import com.payflow.transaction.internal.util.sendRelated.SendMoneyResponse;
import com.payflow.transaction.internal.util.TransactionStatementDTO;
import com.payflow.transaction.internal.util.withdrawalRelated.WithdrawalResponse;
import com.payflow.wallet.api.WalletAdapter;
import com.payflow.wallet.internal.util.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final WalletAdapter walletAdapter;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final ExchangeAdapter exchangeAdapter;
    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    //refactor all protected methods to a new bean to prevent it transactional from being skipped

    public DepositResponse requestDepositMpesa(Long userId, BigDecimal amount, String idempotencyKey, String mpesaPhoneNumber) {
        logger.info("userId={} , amount={} ,idempotencyKey={}, mpesaPhoneNumber={} ", userId, amount, idempotencyKey, mpesaPhoneNumber);

        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new TransactionException("idempotencyKey cannot be null or empty");
        }

        //amount check
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount must be greater than zero");
        }

        if (mpesaPhoneNumber == null || !mpesaPhoneNumber.matches("^254[0-9]{9}$")){
            throw new TransactionException("Invalid mpesa phone number");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        //prevents duplicate transactions
        if (existingTransaction.isPresent()) {
            return mapToResponse(existingTransaction.get());
        }

        WalletInfo walletInfo = walletAdapter.getWalletByUserId(userId);

        CurrencyEnum currencyFrom = CurrencyEnum.KES;
        CurrencyEnum currencyTo;

        try {
            currencyTo = CurrencyEnum.valueOf(walletInfo.currency().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Unsupported wallet currency, " + walletInfo.currency());
        }

        ConversionResult conversion = exchangeAdapter.convert(amount, currencyFrom, currencyTo);

        return persistDepositMpesa(userId,walletInfo,amount,idempotencyKey, currencyFrom, conversion, mpesaPhoneNumber);
    }

    @Transactional
    protected DepositResponse persistDepositMpesa(Long userId,WalletInfo walletInfo,BigDecimal amount,String idempotencyKey, CurrencyEnum currencyFrom, ConversionResult conversion,String mpesaPhoneNumber) {

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

            JsonNode payload = objectMapper.valueToTree(
                    Map.of("transactionId", savedTransaction.getTransactionId(),
                            "mpesaPhoneNumber",mpesaPhoneNumber
            ));

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setStatus(StatusEnum.PENDING);
            outboxEvent.setUserId(userId);
            outboxEvent.setEventType(EventType.STK_PUSH_REQUEST);
            outboxEvent.setPayload(payload);
            outboxEvent.setRetryCount(0);

            outboxRepository.save(outboxEvent);

            return mapToResponse(savedTransaction);
        } catch (DataIntegrityViolationException e) {
            Transaction existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

            return mapToResponse(existingTx);
        }
    }

    public SendMoneyResponse sendMoney(Long senderId, String receiverWalletTag, String idempotencyKey, BigDecimal amount, String description) {
        logger.info("senderId={}, receiverWalletTag={}, idempotencyKey={}, amount={},description={}", senderId, receiverWalletTag, idempotencyKey, amount, description);
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new TransactionException("Idempotency key cannot be null.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount needs to be greater than 0.");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            logger.warn("unable to process your request because a similar transaction is currently underway. Please wait while we complete your initial request.");
            return mapToResponseSend(existingTransaction.get());
        }

        WalletInfo senderWallet = walletAdapter.getWalletByUserId(senderId);
        WalletInfo receiverWallet = walletAdapter.getWalletByWalletTag(receiverWalletTag);

        if (senderWallet.walletId().equals(receiverWallet.walletId())) {
            throw new TransactionException("You cannot send money to your own wallet");
        }

        BigDecimal sendersBalance = walletAdapter.getWalletBalance(senderWallet.walletId());


        CurrencyEnum senderCurrency;
        try {
            senderCurrency = CurrencyEnum.valueOf(senderWallet.currency().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Unsupported sender wallet currency: " + senderWallet.currency());
        }

        CurrencyEnum receiverCurrency;
        try {
            receiverCurrency = CurrencyEnum.valueOf(receiverWallet.currency().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Unsupported receiver wallet currency: " + receiverWallet.currency());
        }

        ConversionResult conversionResult = exchangeAdapter.convert(amount, senderCurrency, receiverCurrency);

        if (amount.compareTo(sendersBalance) > 0) {
            throw new InsufficientFundsException("Insufficient funds.");
        }
        return persistSendMoney(senderId, senderWallet, receiverWallet, amount, conversionResult.destinationAmount(), idempotencyKey, conversionResult.rateUsed(), description);
    }

    @Transactional
    protected SendMoneyResponse persistSendMoney(Long senderId, WalletInfo senderWallet, WalletInfo receiverWallet, BigDecimal sourceAmount, BigDecimal convertedAmount, String idempotencyKey, BigDecimal exchangeRate, String description) {
        try {
            Transaction debitTX = new Transaction();
            debitTX.setSourceWalletId(senderWallet.walletId());
            debitTX.setSourceCurrency(senderWallet.currency());
            debitTX.setSourceAmount(sourceAmount);

            debitTX.setWalletDestinationId(receiverWallet.walletId());
            debitTX.setDestinationCurrency(receiverWallet.currency());
            debitTX.setDestinationAmount(convertedAmount);

            debitTX.setTransactionStatus(TransactionStatus.PENDING);
            debitTX.setTransactionDirection(TransactionDirection.DEBIT);
            debitTX.setPaymentMethod(PaymentMethod.TRANSFER);
            debitTX.setExchangeRate(exchangeRate);
            debitTX.setDescription(description);
            debitTX.setIdempotencyKey(idempotencyKey);

            Transaction creditTx = new Transaction();
            creditTx.setSourceWalletId(senderWallet.walletId());
            creditTx.setSourceCurrency(senderWallet.currency());
            creditTx.setSourceAmount(sourceAmount);

            creditTx.setWalletDestinationId(receiverWallet.walletId());
            creditTx.setDestinationCurrency(receiverWallet.currency());
            creditTx.setDestinationAmount(convertedAmount);

            creditTx.setTransactionStatus(TransactionStatus.PENDING);
            creditTx.setTransactionDirection(TransactionDirection.CREDIT);
            creditTx.setPaymentMethod(PaymentMethod.TRANSFER);
            creditTx.setExchangeRate(exchangeRate);
            creditTx.setDescription(description);
            creditTx.setIdempotencyKey(idempotencyKey + ":CREDIT");

            Transaction savedTransaction = transactionRepository.save(debitTX);
            Transaction savedTransactionCredit = transactionRepository.save(creditTx);

            logger.info("debitTransactionId={},creditTransactionId={}", savedTransaction.getTransactionId(), savedTransactionCredit.getTransactionId());

            JsonNode payload = objectMapper.
                    valueToTree(
                            Map.of("debitTransactionId", savedTransaction.getTransactionId(),
                                    "creditTransactionId", savedTransactionCredit.getTransactionId(),
                                    "senderWalletId", senderWallet.walletId(),
                                    "receiverWalletId", receiverWallet.walletId(),
                                    "sourceAmount", sourceAmount,
                                    "convertedAmount", convertedAmount));

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setStatus(StatusEnum.PENDING);
            outboxEvent.setUserId(senderId);
            outboxEvent.setEventType(EventType.TRANSFER_REQUEST);
            outboxEvent.setPayload(payload);
            outboxEvent.setRetryCount(0);

            outboxRepository.save(outboxEvent);
            logger.info("outboxEvent for transaction created");

            return mapToResponseSend(savedTransaction);
        } catch (DataIntegrityViolationException ex) {
            Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

            return mapToResponseSend(transaction);
        }

    }

    public WithdrawalResponse withdrawMoneyMpesa(Long senderId, BigDecimal amount, String idempotencyKey, String mpesaPhoneNumber, String description) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new TransactionException("IdempotencyKey cannot be null");
        }

        if (mpesaPhoneNumber == null || !mpesaPhoneNumber.matches("^254[0-9]{9}$")){
            throw new TransactionException("Invalid mpesa phone number."+mpesaPhoneNumber);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Amount has to be higher than 0.");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            return mapToResponseWithdraw(existingTransaction.get());
        }

        WalletInfo senderWallet = walletAdapter.getWalletByUserId(senderId);
        BigDecimal senderBalance = walletAdapter.getWalletBalance(senderWallet.walletId());

        CurrencyEnum senderWalletCurrency;
        CurrencyEnum mpesaCurrency = CurrencyEnum.KES;

        try {
            senderWalletCurrency = CurrencyEnum.valueOf(senderWallet.currency().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TransactionException("Unsupported sender wallet currency: " + senderWallet.currency());
        }

        if (amount.compareTo(senderBalance) > 0){
            throw new InsufficientFundsException("Insufficient funds.");
        }
        ConversionResult conversionResult = exchangeAdapter.convert(amount, senderWalletCurrency, mpesaCurrency);

        return persistWithdrawalMpesa(senderId, senderWallet, amount, conversionResult.destinationAmount(), conversionResult.rateUsed(), description, idempotencyKey, mpesaPhoneNumber);
    }

    @Transactional
    protected WithdrawalResponse persistWithdrawalMpesa(Long senderId, WalletInfo senderWallet, BigDecimal sourceAmount, BigDecimal destinationAmount, BigDecimal rateUsed, String description, String idempotencyKey, String mpesaPhoneNumber) {
        try {
            Transaction transaction = new Transaction();
            transaction.setIdempotencyKey(idempotencyKey);
            transaction.setDescription(description);
            transaction.setPaymentMethod(PaymentMethod.WITHDRAWAL);
            transaction.setTransactionDirection(TransactionDirection.DEBIT);
            transaction.setExchangeRate(rateUsed);
            transaction.setTransactionStatus(TransactionStatus.PENDING);

            transaction.setSourceWalletId(senderWallet.walletId());
            transaction.setSourceCurrency(senderWallet.currency());
            transaction.setSourceAmount(sourceAmount);
            transaction.setDestinationAmount(destinationAmount);
            transaction.setDestinationCurrency(CurrencyEnum.KES.name());

            Transaction savedTransaction = transactionRepository.save(transaction);

            JsonNode payload = objectMapper.valueToTree(
                    Map.of(
                            "transactionId", savedTransaction.getTransactionId(),
                            "mpesaPhoneNumber", mpesaPhoneNumber
                    )
            );

            OutboxEvent event = new OutboxEvent();
            event.setStatus(StatusEnum.PENDING);
            event.setPayload(payload);
            event.setUserId(senderId);
            event.setEventType(EventType.WITHDRAWAL_REQUEST);
            event.setRetryCount(0);


            outboxRepository.save(event);
            return mapToResponseWithdraw(transaction);
        } catch (DataIntegrityViolationException ex) {
            Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();
            return mapToResponseWithdraw(transaction);
        }
    }

    public Page<TransactionStatementDTO> getTransactionStatement(Long walletId){
        Pageable pageable = PageRequest.of(0,10, Sort.by("transactionAt").ascending());
        Page<Transaction> transactionPage = transactionRepository.findBySourceWalletId(walletId,pageable);

        List<Transaction> transactionList = transactionPage.getContent();

        if(transactionList.isEmpty()){
            logger.info("List is empty");
            return new PageImpl<>(List.of(),pageable,transactionPage.getTotalElements());
        }

        return new PageImpl<>(transactionStatementDTOList(transactionList));
    }

    public TransactionStatementDTO getTransactionStatementById(Long walletId,Long transactionId){
        Transaction transaction = transactionRepository.findTransactionForWalletById(walletId,transactionId).orElseThrow(() -> new TransactionException("Transaction not found"));

        return convertToDTO(transaction);
    }

    private WithdrawalResponse mapToResponseWithdraw(Transaction existingTransaction) {
        return new WithdrawalResponse(
                existingTransaction.getExternalReference(),
                existingTransaction.getDestinationAmount(),
                existingTransaction.getTransactionAt(),
                existingTransaction.getDescription()
        );
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

    private SendMoneyResponse mapToResponseSend(Transaction transaction) {
        return new SendMoneyResponse(
                transaction.getWalletDestinationId(),
                transaction.getDestinationAmount(),
                transaction.getTransactionAt(),
                transaction.getExternalReference()
        );
    }

    private TransactionStatementDTO convertToDTO(Transaction transaction){
        return new TransactionStatementDTO(
                transaction.getTransactionId(),
                transaction.getSourceWalletId(),
                transaction.getWalletDestinationId(),
                transaction.getSourceAmount(),
                transaction.getTransactionStatus().name(),
                transaction.getTransactionAt()
        );
    }

    private List<TransactionStatementDTO> transactionStatementDTOList(List<Transaction> transactions){
        return transactions
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}
