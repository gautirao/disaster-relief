package com.disasterrelief.core.saga;

import com.disasterrelief.core.event.DomainEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class SagaManager<ID, S extends Saga<ID>> {

    private final Map<ID, S> sagas = new HashMap<>();
    private final Function<DomainEvent, ID> sagaIdExtractor;
    private final Function<ID, S> sagaFactory;
    private final Predicate<DomainEvent> isStartingEvent;

    /**
     * @param sagaIdExtractor function to extract saga ID from an event
     * @param sagaFactory function to create new saga instances for a given ID
     * @param isStartingEvent predicate to identify if an event starts a new saga
     */
    public SagaManager(Function<DomainEvent, ID> sagaIdExtractor,
                       Function<ID, S> sagaFactory,
                       Predicate<DomainEvent> isStartingEvent) {
        this.sagaIdExtractor = sagaIdExtractor;
        this.sagaFactory = sagaFactory;
        this.isStartingEvent = isStartingEvent;
    }

    /**
     * Dispatch an event to the relevant saga instance.
     * Creates saga if none exists and event is starting event.
     */
    public void handleEvent(DomainEvent event) {
        if (event == null) return;

        ID sagaId = sagaIdExtractor.apply(event);
        if (sagaId == null) return;

        S saga = sagas.get(sagaId);

        if (saga == null) {
            // Only create saga if event is a starting event
            if (!isStartingEvent.test(event)) {
                return; // ignore event for unknown saga that is not a start event
            }
            saga = sagaFactory.apply(sagaId);
            sagas.put(sagaId, saga);
        }

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
        if (events == null) return;
        for (DomainEvent event : events) {
            handleEvent(event);
        }
    }

    public Map<ID, S> getActiveSagas() {
        return Map.copyOf(sagas);
    }
}
