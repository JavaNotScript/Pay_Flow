package com.payflow.wallet.api;

import com.payflow.common.ex.WalletCreationEx;
import com.payflow.common.ex.WalletNotFoundEx;
import com.payflow.transaction.api.TransactionAdapter;
import com.payflow.transaction.internal.util.TransactionDTO;
import com.payflow.wallet.internal.domain.Wallet;
import com.payflow.wallet.internal.repos.WalletRepository;
import com.payflow.wallet.internal.services.WalletService;
import com.payflow.wallet.internal.util.WalletInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class WalletAdapter implements WalletFacade {
    private final WalletRepository walletRepository;
    private final TransactionAdapter transactionAdapter;
    private final WalletService walletService;
    private final Logger logger = LoggerFactory.getLogger(WalletAdapter.class);


    @Override
    public WalletInfo getWalletByUserId(Long userId) {
        try {
            Wallet wallet = walletRepository.findByOwnerId(userId).orElseThrow(() -> new WalletCreationEx("Wallet not found for userID "+userId));

            return new WalletInfo(
                    wallet.getWalletId(),
                    wallet.getCurrency().name(),
                    wallet.getWalletTag()
            );

        }catch (WalletNotFoundEx e){
            logger.error("Wallet not found for userId={}",userId);
            throw new WalletCreationEx(e.getMessage());
        }
    }

    @Override
    public WalletInfo getWalletByWalletTag(String receiverWalletTag) {
        try {
            Wallet wallet = walletRepository.findByWalletTag(receiverWalletTag)
                    .orElseThrow();

            return new WalletInfo(
                    wallet.getWalletId(),
                    wallet.getCurrency().name(),
                    wallet.getWalletTag()
            );
        }catch (WalletNotFoundEx e){
            logger.error("Wallet not found= {}",receiverWalletTag);
            throw new WalletCreationEx(e.getMessage());
        }
    }

    public BigDecimal getWalletBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletCreationEx("Wallet not found"));

        return wallet.getBalance();
    }

    public void reverseDebit(Long transactionId){
        //get transaction
        //get amount
        //get wallet sourceId
        //send walletId and amount back to wallet service for debit

        TransactionDTO transactionDTO = transactionAdapter.findTransactionById(transactionId);

        walletService.reverseDebit(transactionDTO.walletSourceId(),transactionDTO.sourceAmount());
    }

    public void depositRequest(Long walletId, BigDecimal amount) {
        walletService.depositRequest(walletId,amount);
    }
}
