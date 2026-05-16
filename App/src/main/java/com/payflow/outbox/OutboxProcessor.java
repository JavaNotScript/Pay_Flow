package com.payflow.outbox;

import com.payflow.common.domain.EventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);


    @Scheduled(fixedDelay = 5000)
    public void processEvents() {
        Pageable pageRequest = PageRequest.of(0, 30, Sort.by("createdAt").ascending());
        Page<OutboxEvent> outboxEvents = outboxRepository.findByStatusAndEventTypeOrderByCreatedAtAsc(StatusEnum.PENDING, EventType.USER_CREATED, pageRequest);

        List<OutboxEvent> outboxEventList = outboxEvents.getContent();

        for (OutboxEvent event : outboxEventList) {
            eventHandler.processSingleEvent(event);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processDepositRequests() {
        Pageable pageRequest = PageRequest.of(0, 20, Sort.by("createdAt").ascending());
        Page<OutboxEvent> events = outboxRepository.findByStatusAndEventTypeOrderByCreatedAtAsc(StatusEnum.PENDING, EventType.DEPOSIT_REQUEST, pageRequest);

        List<OutboxEvent> outboxEventList = events.getContent();

        for (OutboxEvent event : outboxEventList) {
            eventHandler.processDepositEvent(event);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processSendMoneyTransferRequest(){
        Pageable pageable = PageRequest.of(0,30,Sort.by("createdAt").ascending());
        Page<OutboxEvent> outboxEvents = outboxRepository.findByStatusAndEventTypeOrderByCreatedAtAsc(StatusEnum.PENDING,EventType.TRANSFER_REQUEST,pageable);
        List<OutboxEvent> outboxEventList = outboxEvents.getContent();


        for (OutboxEvent event:outboxEventList){
            eventHandler.processSendMoneyTransferRequests(event);
        }
    }

}
