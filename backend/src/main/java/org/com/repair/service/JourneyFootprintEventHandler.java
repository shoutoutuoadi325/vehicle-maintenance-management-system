package org.com.repair.service;

import org.com.repair.entity.JourneyFootprint;
import org.com.repair.event.JourneyFootprintEvent;
import org.com.repair.repository.JourneyFootprintRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JourneyFootprintEventHandler {

    private final JourneyFootprintRepository journeyFootprintRepository;

    public JourneyFootprintEventHandler(JourneyFootprintRepository journeyFootprintRepository) {
        this.journeyFootprintRepository = journeyFootprintRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(JourneyFootprintEvent event) {
        JourneyFootprint footprint = new JourneyFootprint();
        footprint.setUserId(event.userId());
        footprint.setMapId(event.mapId());
        footprint.setEventType(event.eventType());
        footprint.setEventDescription(event.eventDescription());
        journeyFootprintRepository.save(footprint);
    }
}
