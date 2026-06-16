package com.payflow.transaction.internal.repos;

import com.payflow.transaction.internal.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<Transaction> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWalletId= :walletId OR t.walletDestinationId= :walletId ORDER BY t.transactionAt DESC")
    Page<Transaction> findBySourceWalletId(@Param("walletId") Long walletId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t WHERE t.transactionId= :transactionId AND (t.sourceWalletId= :walletId OR t.walletDestinationId= :walletId)""")
    Optional<Transaction> findTransactionForWalletById(Long walletId, Long transactionId);
}
