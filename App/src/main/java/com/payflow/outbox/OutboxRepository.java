package com.payflow.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT e FROM OutboxEvent e\s
            WHERE e.status = 'PENDING'\s
            AND (e.lastAttemptAt IS NULL OR e.lastAttemptAt < CURRENT_TIMESTAMP)
           \s""")
    Page<OutboxEvent> findPending(Pageable pageable);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND e.eventType = 'DEPOSIT_REQUEST'")
    Page<OutboxEvent> findPendingTransactions(Pageable pageRequest);
}
