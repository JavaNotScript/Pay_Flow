package com.payflow.wallet.api;

import com.payflow.outbox.UserCreatedEvent;
import com.payflow.wallet.internal.services.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WalletEventListener {
    private final WalletService walletService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvent(UserCreatedEvent event) {
        logger.info("Transaction active: {}",
                org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());

        try {
            walletService.createWallet(event.userId(), event.walletTag());
        }catch (Exception e) {
            logger.error("Error while creating wallet for userId={}", event.userId(), e);
        }
    }
}
