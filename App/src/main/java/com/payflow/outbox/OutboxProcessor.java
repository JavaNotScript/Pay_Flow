package com.payflow.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxProcessor {
    private final OutboxRepository outboxRepository;
    private final EventHandler eventHandler;


    @Scheduled(fixedDelay = 5000)
    public void processEvents() {
        Pageable pageRequest = PageRequest.of(0, 30, Sort.by("createdAt").ascending());
        Page<OutboxEvent> outboxEvents = outboxRepository.findPending(pageRequest);

        List<OutboxEvent> outboxEventList = outboxEvents.getContent();

        for (OutboxEvent event : outboxEventList) {
            eventHandler.processSingleEvent(event);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processDepositRequests(){
        Pageable pageRequest = PageRequest.of(0, 20, Sort.by("transactionAt").ascending());
        Page<OutboxEvent> events = outboxRepository.findPendingTransactions(pageRequest);

        List<OutboxEvent> outboxEventList = events.getContent();

        for (OutboxEvent event : outboxEventList) {
            eventHandler.processDepositEvent(event);
        }


    }


}
