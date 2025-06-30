package com.disasterrelief.core.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.disasterrelief.core.event.DomainEvent;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = EventStoreIntegrationTest.Initializer.class)
@Testcontainers
public class EventStoreIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext ctx) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.jpa.hibernate.ddl-auto=create-drop"
            ).applyTo(ctx.getEnvironment());
        }
    }

    @Resource
    private JpaEventStore eventStore;

    @Test
    @Transactional
    void persistsAndReadsBackDomainEvent() {
        UUID commandId = UUID.randomUUID();

        DomainEvent event = new DummyEvent(commandId, Instant.now());

        eventStore.append( event);

        List<DomainEvent> events = eventStore.readByAggregateId(commandId);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().aggregateId()).isEqualTo(commandId);
        assertThat(events.getFirst()).isInstanceOf(DummyEvent.class);
        assertThat(events.getFirst().occurredAt()).isNotNull();

    }

    public record DummyEvent(UUID aggregateId, Instant occurredAt) implements DomainEvent {}
}
