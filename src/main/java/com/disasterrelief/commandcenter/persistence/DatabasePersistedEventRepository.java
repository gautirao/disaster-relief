package com.disasterrelief.commandcenter.persistence;

import com.disasterrelief.core.eventstore.PersistedEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DatabasePersistedEventRepository implements PersistedEventRepository {

    private final JpaPersistedEventRepository jpaRepo;

    public DatabasePersistedEventRepository(JpaPersistedEventRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void save(PersistedEvent event) {
        jpaRepo.save(event);
    }

    @Override
    public List<PersistedEvent> findBySagaId(UUID sagaId) {
        return jpaRepo.findBySagaId(sagaId);
    }
}
