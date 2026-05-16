package com.payflow.wallet.internal.repos;

import com.payflow.wallet.internal.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByOwnerId(Long ownerId);

    @Query("SELECT w FROM Wallet w WHERE w.ownerId =:ownerId")
    Optional<Wallet> findByOwnerId(@Param("ownerId") Long ownerId);

    Optional<Wallet> findByWalletTag(String receiverWalletTag);
}
