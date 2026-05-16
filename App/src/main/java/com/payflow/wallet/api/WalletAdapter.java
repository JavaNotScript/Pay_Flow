package com.payflow.wallet.api;

import com.payflow.common.ex.WalletCreationEx;
import com.payflow.common.ex.WalletNotFoundEx;
import com.payflow.wallet.internal.domain.Wallet;
import com.payflow.wallet.internal.repos.WalletRepository;
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
    private final Logger logger = LoggerFactory.getLogger(WalletAdapter.class);


    @Override
    public WalletInfo getWalletByUserId(Long userId) {
        try {
            Wallet wallet = walletRepository.findByOwnerId(userId).orElseThrow();

            return new WalletInfo(
                    wallet.getWalletId(),
                    wallet.getCurrency().name()
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
                    wallet.getCurrency().name()
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
}
