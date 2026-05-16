package com.payflow.wallet.internal.services;

import com.payflow.common.ex.InsufficientFundsException;
import com.payflow.common.ex.WalletCreationEx;
import com.payflow.common.ex.WalletNotFoundEx;
import com.payflow.wallet.internal.domain.CurrencyEnum;
import com.payflow.wallet.internal.domain.StatusEnum;
import com.payflow.wallet.internal.domain.Wallet;
import com.payflow.wallet.internal.dto.WalletDTO;
import com.payflow.wallet.internal.repos.WalletRepository;
import com.payflow.wallet.internal.util.UpdateCurrencyResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@EnableRetry
public class WalletService {
    private final WalletRepository walletRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PersistenceContext
    private EntityManager entityManager;

    public void createWallet(Long ownerId, String walletTag) {
        logger.info("Creating wallet for owner id={}", ownerId);

        if (walletRepository.existsByOwnerId(ownerId)) {
            logger.info("user already has a wallet created. ownerId={}", ownerId);
            return;
        }

        Wallet wallet = new Wallet();
        wallet.setOwnerId(ownerId);
        wallet.setWalletTag(walletTag);
        wallet.setCurrency(CurrencyEnum.USD); //always the default currency
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatusEnum(StatusEnum.ACTIVE);

        entityManager.persist(wallet);
        entityManager.flush();

        Wallet createdWallet = walletRepository.save(wallet);

        logger.info("wallet created walletId={}", createdWallet.getWalletId());

    }

    public WalletDTO getCurrentWallet(Long ownerId) {
        Wallet wallet = walletRepository.findByOwnerId(ownerId).orElseThrow(() -> new WalletNotFoundEx("wallet not found"));

        return new WalletDTO(
                wallet.getWalletId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getOwnerId()
        );
    }

    @Transactional
    public UpdateCurrencyResponse updateCurrency(Long ownerId, String currency) {
        Wallet wallet = walletRepository.findByOwnerId(ownerId).orElseThrow(() -> new WalletNotFoundEx("wallet not found"));

        CurrencyEnum currencyEnum;

        try {
            currencyEnum = CurrencyEnum.valueOf(currency.toUpperCase());

        } catch (IllegalArgumentException e) {
            logger.info("currency not found. ownerId={},currency={}", ownerId, currency, e);
            throw new IllegalArgumentException("Invalid currency: " + currency);
        }

        wallet.setCurrency(currencyEnum);


        return new UpdateCurrencyResponse(
                wallet.getWalletId(),
                wallet.getCurrency().name()
        );
    }

    public void depositRequest(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        // "If the wallet has an existing balance use it, otherwise start from zero"
        BigDecimal current = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(current.add(amount));

        walletRepository.save(wallet);

    }

    public void transferRequest(Long creditWalletId, Long senderWalletId, BigDecimal debitAmount, BigDecimal creditAmount) {
        Wallet senderWallet = walletRepository.findById(senderWalletId)
                .orElseThrow(() -> new WalletCreationEx("Wallet not found :" + senderWalletId));

        BigDecimal currentBalance = senderWallet.getBalance() != null ? senderWallet.getBalance() : BigDecimal.ZERO;

        if (currentBalance.compareTo(debitAmount) < 0) {
            throw new InsufficientFundsException("Insufficient funds at execution time. Balance: "
                    + currentBalance + ", Required: " + debitAmount);
        }

        senderWallet.setBalance(currentBalance.subtract(debitAmount));

        walletRepository.save(senderWallet);
        logger.info("Debit wallet balance deducted and updated successfully");

        Wallet receiverWallet = walletRepository.findById(creditWalletId)
                .orElseThrow(() -> new WalletCreationEx("Wallet not found :" + creditWalletId));

        BigDecimal walletBalance = receiverWallet.getBalance() != null ? receiverWallet.getBalance() : BigDecimal.ZERO;

        receiverWallet.setBalance(walletBalance.add(creditAmount));

        walletRepository.save(receiverWallet);
        logger.info("credit wallet balance added and updated successfully");
    }
}
