package com.payflow.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.payflow.common.ex.MpesaWafBlockException;
import com.payflow.common.ex.TransactionException;
import com.payflow.mpesa.service.MpesaB2CService;
import com.payflow.transaction.api.TransactionFacade;
import com.payflow.transaction.internal.util.TransactionDTO;
import com.payflow.wallet.internal.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventHandler {
    private final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    private final WalletService walletService;
    private final OutboxRepository outboxRepository;
    private final TransactionFacade transactionFacade;
    private final MpesaB2CService mpesaB2CService;

    @Transactional
    public void processSingleEvent(OutboxEvent event) {

        event.setStatus(StatusEnum.PROCESSING);
        outboxRepository.save(event);

        try {
            walletService.createWallet(event.getUserId(), event.getWalletTag());

            event.setStatus(StatusEnum.PROCESSED);
            event.setProcessedAt(OffsetDateTime.now());
            outboxRepository.save(event);


            logger.info("Marking event {} as PROCESSED", event.getEventId());


        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastAttemptAt(OffsetDateTime.now());
            event.setErrorMessage(ex.getMessage());

            if (event.getRetryCount() >= 5) {
                event.setStatus(StatusEnum.FAILED);
            } else {
                event.setStatus(StatusEnum.PENDING);
            }
        }
        outboxRepository.save(event);
    }

    @Transactional
    public void processDepositEvent(OutboxEvent event) {
        OutboxEvent lockedEvent = outboxRepository.findByIdWithLock(event.getEventId())
                .orElseThrow();

        if (lockedEvent.getStatus() != StatusEnum.PENDING) {
            return;
        }

        Long transactionId = event.getPayload().get("transactionId").asLong();
        TransactionDTO transactionDTO = transactionFacade.findTransactionById(transactionId);
        transactionFacade.updateTransactionStatus(transactionId, "PROCESSING");

        try {
            walletService.depositRequest(transactionDTO.walletDestinationId(), transactionDTO.destinationAmount());

            lockedEvent.setStatus(StatusEnum.PROCESSED);
            lockedEvent.setProcessedAt(OffsetDateTime.now());

            transactionFacade.updateTransactionStatus(transactionId, "SUCCESS");

        } catch (Exception ex) {
            lockedEvent.setRetryCount(lockedEvent.getRetryCount() + 1);
            lockedEvent.setLastAttemptAt(OffsetDateTime.now());
            lockedEvent.setErrorMessage(ex.getMessage());

            if (lockedEvent.getRetryCount() >= 5) {
                lockedEvent.setStatus(StatusEnum.FAILED);
                transactionFacade.updateTransactionStatus(transactionId, "FAILED");
            } else {
                lockedEvent.setStatus(StatusEnum.PENDING);
            }
        }
        outboxRepository.save(lockedEvent);
    }

    @Transactional
    public void processSendMoneyTransferRequests(OutboxEvent event) {
        OutboxEvent lockedEvent = outboxRepository.findByIdWithLock(event.getEventId())
                .orElseThrow();

        if (lockedEvent.getStatus() != StatusEnum.PENDING) {
            return;
        }

        JsonNode payload = event.getPayload();

        Long debitTransactionId = Optional.ofNullable(payload.get("debitTransactionId"))
                .map(JsonNode::asLong)
                .orElseThrow(() -> new TransactionException("Missing debitTransactionId in payload: " + payload));

        Long creditTransactionId = Optional.ofNullable(payload.get("creditTransactionId"))
                .map(JsonNode::asLong)
                .orElseThrow(() -> new TransactionException("Missing creditTransactionId in payload: " + payload));


        TransactionDTO debitTransactionDTO = transactionFacade.findTransactionById(debitTransactionId);
        TransactionDTO creditTransactionDTO = transactionFacade.findTransactionById(creditTransactionId);

        transactionFacade.updateTransactionStatus(debitTransactionId, "PROCESSING");
        transactionFacade.updateTransactionStatus(creditTransactionId, "PROCESSING");

        try {
            walletService.transferRequest(creditTransactionDTO.walletDestinationId(), debitTransactionDTO.walletSourceId(), debitTransactionDTO.sourceAmount(), creditTransactionDTO.destinationAmount());
            lockedEvent.setStatus(StatusEnum.PROCESSED);
            lockedEvent.setProcessedAt(OffsetDateTime.now());

            transactionFacade.updateTransactionStatus(debitTransactionId, "SUCCESS");
            transactionFacade.updateTransactionStatus(creditTransactionId, "SUCCESS");
        } catch (Exception ex) {
            lockedEvent.setRetryCount(lockedEvent.getRetryCount() + 1);
            lockedEvent.setLastAttemptAt(OffsetDateTime.now());
            lockedEvent.setErrorMessage(ex.getMessage());

            if (lockedEvent.getRetryCount() >= 5) {
                logger.warn("EventId={} failed, error message={}", lockedEvent.getEventId(), lockedEvent.getErrorMessage());
                lockedEvent.setStatus(StatusEnum.FAILED);

                transactionFacade.updateTransactionStatus(debitTransactionId, "FAILED");
                transactionFacade.updateTransactionStatus(creditTransactionId, "FAILED");
            } else {
                lockedEvent.setStatus(StatusEnum.PENDING);
            }

        }

        outboxRepository.save(lockedEvent);
    }

    @Transactional
    public void processWithdrawalTransferRequests(OutboxEvent event) {
        OutboxEvent lockedEvent = outboxRepository.findByIdWithLock(event.getEventId()).orElseThrow();

        if (lockedEvent.getStatus() != StatusEnum.PENDING) {
            return;
        }

        JsonNode payload = lockedEvent.getPayload();

        Long transactionId = Optional.ofNullable(payload.get("transactionId"))
                .map(JsonNode::asLong)
                .orElseThrow(() -> new TransactionException("payload for mpesa withdrawal missing transactionId"));

        String phoneNumber = Optional.ofNullable(payload.get("mpesaPhoneNumber"))
                .map(JsonNode::asText)
                .orElseThrow(() -> new TransactionException("payload for mpesa withdrawal transaction missing receivers phone number"));

        TransactionDTO transactionDTO = transactionFacade.findTransactionById(transactionId);

        try {
            walletService.debitWallet(transactionDTO.walletSourceId(), transactionDTO.sourceAmount());

            mpesaB2CService.sendB2C(phoneNumber, transactionDTO.destinationAmount(), String.valueOf(transactionId), transactionDTO.description());

            transactionFacade.updateTransactionStatus(transactionId, "PROCESSING");

            lockedEvent.setStatus(StatusEnum.PROCESSED);
            lockedEvent.setProcessedAt(OffsetDateTime.now());
        } catch (MpesaWafBlockException ex) {
            walletService.reverseDebit(transactionDTO.walletSourceId(), transactionDTO.sourceAmount());
            lockedEvent.setStatus(StatusEnum.PENDING);
            lockedEvent.setErrorMessage(ex.getMessage());
            lockedEvent.setLastAttemptAt(OffsetDateTime.now());

            // do NOT increment retryCount
            logger.warn("WAF block detected, pausing withdrawal event id={}", lockedEvent.getEventId());

        } catch (Exception ex) {
            // reverse debit if M-Pesa call failed
            walletService.reverseDebit(transactionDTO.walletSourceId(), transactionDTO.sourceAmount());

            lockedEvent.setRetryCount(lockedEvent.getRetryCount() + 1);
            lockedEvent.setLastAttemptAt(OffsetDateTime.now());
            lockedEvent.setErrorMessage(ex.getMessage());
            lockedEvent.setStatus(lockedEvent.getRetryCount() >= 5
                    ? StatusEnum.FAILED : StatusEnum.PENDING);
            logger.info("withdrawal request failed, transactionId={},amount={},walletId={},error={}", transactionId, transactionDTO.destinationAmount(), transactionDTO.walletSourceId(), ex.getMessage(), ex);
        }

        outboxRepository.save(lockedEvent);

    }
}
