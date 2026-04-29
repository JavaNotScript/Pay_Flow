package com.payflow.outbox;

import com.payflow.transaction.api.TransactionFacade;
import com.payflow.transaction.internal.util.TransactionDTO;
import com.payflow.wallet.internal.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class EventHandler {
    private final Logger logger = LoggerFactory.getLogger(EventHandler.class);
    private final WalletService walletService;
    private final OutboxRepository outboxRepository;
    private final TransactionFacade transactionFacade;

    @Transactional
    public void processSingleEvent(OutboxEvent event) {

        event.setStatus(StatusEnum.PROCESSING);
        outboxRepository.save(event);

        try {
            walletService.createWallet(event.getUserId());

            event.setStatus(StatusEnum.PROCESSED);
            event.setProcessedAt(OffsetDateTime.now());
            outboxRepository.save(event);


            logger.info("Marking event {} as PROCESSED", event.getEvent_id());


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
        event.setStatus(StatusEnum.PENDING);
        outboxRepository.save(event);

        Long transactionId = event.getPayload().get("transactionId").asLong();
        TransactionDTO transactionDTO = transactionFacade.findTransactionById(transactionId);

        try {
            walletService.depositRequest(transactionDTO.walletId(), transactionDTO.currency(), transactionDTO.amount());

            event.setStatus(StatusEnum.PROCESSED);
            event.setProcessedAt(OffsetDateTime.now());

            outboxRepository.save(event);
        } catch (Exception ex) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setLastAttemptAt(OffsetDateTime.now());
            event.setErrorMessage(ex.getMessage());

            if (event.getRetryCount() >= 5) {
                event.setStatus(StatusEnum.FAILED);
            }else {
                event.setStatus(StatusEnum.PENDING);
            }
        }
        outboxRepository.save(event);
    }
}
