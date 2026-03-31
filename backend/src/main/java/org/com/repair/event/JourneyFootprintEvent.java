package org.com.repair.event;

public record JourneyFootprintEvent(
        Long userId,
        Long mapId,
        String eventType,
        String eventDescription
) {
}
