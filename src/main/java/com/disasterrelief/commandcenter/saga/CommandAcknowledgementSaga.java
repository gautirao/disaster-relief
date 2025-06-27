package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.domain.event.CommandIssuedEvent;
import com.disasterrelief.core.event.DomainEvent;
import com.disasterrelief.core.saga.Saga;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandAcknowledgementSaga implements Saga<UUID> {

    private final UUID sagaId;
    private Set<UUID> expectedAcknowledgerIds = new HashSet<>();
    private Set<UUID> acknowledgedIds = new HashSet<>();
    private boolean completed = false;

    public CommandAcknowledgementSaga() {
        this.sagaId = null; // will be set when handling first event
    }

    public CommandAcknowledgementSaga(UUID sagaId) {
        this.sagaId = sagaId;
    }

    @Override
    public UUID getId() {
        return sagaId;
    }

    @Override
    public void handle(DomainEvent event) {
        if (completed) return; // no further processing if saga is done

        if (event instanceof CommandIssuedEvent issued) {
            // Start saga, set expected acknowledgers
            expectedAcknowledgerIds = new HashSet<>(issued.expectedAcknowledgerIds());
        } else if (event instanceof CommandAcknowledgedEvent ack) {
            // Track acknowledged member
            acknowledgedIds.add(ack.memberId());

            // Complete saga if all expected have acknowledged
            if (acknowledgedIds.containsAll(expectedAcknowledgerIds)) {
                completed = true;
            }
        }
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
