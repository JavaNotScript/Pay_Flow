package com.payflow.outbox;

import com.payflow.common.domain.EventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT e FROM OutboxEvent e\s
            WHERE e.status = 'PENDING'\s
            AND (e.lastAttemptAt IS NULL OR e.lastAttemptAt < CURRENT_TIMESTAMP)
           \s""")
    Page<OutboxEvent> findPending(Pageable pageable);

    Page<OutboxEvent> findByStatusAndEventTypeOrderByCreatedAtAsc(
            StatusEnum status,
            EventType eventType,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvent o WHERE o.eventId= :eventId")
    Optional<OutboxEvent> findByIdWithLock(Long eventId);
}
