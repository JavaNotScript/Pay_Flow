package com.payflow.wallet.api.event;

import com.payflow.common.ex.WalletCreationEx;
import com.payflow.outbox.UserCreatedEvent;
import com.payflow.wallet.internal.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCreatedEventHandler {
    private final WalletService walletService;
    private final Logger logger = LoggerFactory.getLogger(UserCreatedEventHandler.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(UserCreatedEvent userCreatedEvent) {
        try {
            walletService.createWallet(userCreatedEvent.userId(), userCreatedEvent.walletTag());
        } catch (WalletCreationEx e) {
            logger.error("Failed to create a wallet for userId={}",userCreatedEvent.userId());
            throw new RuntimeException(e);
        }
    }
}
