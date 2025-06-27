package com.disasterrelief.core.saga;

import com.disasterrelief.core.event.DomainEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class SagaManager<ID, S extends Saga<ID>> {

    private final Map<ID, S> sagas = new HashMap<>();
    private final Function<DomainEvent, ID> sagaIdExtractor;
    private final Function<ID, S> sagaFactory;

    /**
     * @param sagaIdExtractor function to extract saga ID from an event
     * @param sagaFactory function to create new saga instances for a given ID
     */
    public SagaManager(Function<DomainEvent, ID> sagaIdExtractor, Function<ID, S> sagaFactory) {
        this.sagaIdExtractor = sagaIdExtractor;
        this.sagaFactory = sagaFactory;
    }

    /**
     * Dispatch an event to the relevant saga instance.
     * Creates saga if none exists.
     */
    public void handleEvent(DomainEvent event) {
        ID sagaId = sagaIdExtractor.apply(event);
        if (sagaId == null) return;

        S saga = sagas.computeIfAbsent(sagaId, sagaFactory);
        saga.handle(event);

        if (saga.isCompleted()) {
            sagas.remove(sagaId);
            // Optional: publish saga completion event or cleanup
        }
    }

    /**
     * Replay a list of events (e.g., on startup) to restore saga state.
     */
    public void replayEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            handleEvent(event);
        }
    }

    public Map<ID, S> getActiveSagas() {
        return Map.copyOf(sagas);
    }
}
