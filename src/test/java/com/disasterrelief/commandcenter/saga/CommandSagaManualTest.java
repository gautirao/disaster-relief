package com.disasterrelief.commandcenter.saga;

import com.disasterrelief.commandcenter.domain.event.CommandAcknowledgedEvent;
import com.disasterrelief.commandcenter.persistence.PersistedEventRepository;
import com.disasterrelief.core.saga.CompensationHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommandSagaManualTest {

    @Autowired
    private PersistedEventRepository repository;

    @Test
    void testSagaWithRealPostgres() {
        UUID commandId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Instant deadline = Instant.now().plusSeconds(60);

        CompensationHandler<UUID> compensationHandler = (id, reason) ->
                System.out.println("Compensating for command " + id + " due to " + reason);

        CommandSaga saga = new CommandSaga(commandId, teamId, Set.of(member), deadline, compensationHandler, Clock.systemUTC(), repository);

        saga.handle(new CommandAcknowledgedEvent(commandId, teamId, member, Instant.now()));

        assertThat(repository.findBySagaId(commandId)).hasSize(1);

        CommandSaga loaded = CommandSaga.loadFromEvents(commandId, teamId, Set.of(member), deadline, compensationHandler, repository, Clock.systemUTC());

        assertThat(loaded.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(loaded.getAcknowledgedBy()).containsExactly(member);
    }
}
