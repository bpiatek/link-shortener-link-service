package pl.bpiatek.linkshortenerlinkservice.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

class OutboxRepository {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    OutboxRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    void saveEvent(String aggregateId, LinkEventType eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            jdbcTemplate.update(
                    "INSERT INTO outbox_events (id, aggregate_id, topic, event_type, payload, processed, created_at) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?)",
                    UUID.randomUUID(), aggregateId, "link-lifecycle-events", eventType.name(), jsonPayload, false, Timestamp.from(clock.instant())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize and save Outbox event", e);
        }
    }

    List<OutboxRecord> fetchStalledEvents(int batchSize) {
        return jdbcTemplate.query(
                """
                SELECT id, aggregate_id, topic, event_type, payload
                FROM outbox_events 
                WHERE processed = false 
                  AND created_at < NOW() - INTERVAL '10 seconds' 
                LIMIT ? FOR UPDATE SKIP LOCKED
                """,
                (rs, row) -> new OutboxRecord(
                        rs.getString("id"),
                        rs.getString("aggregate_id"),
                        rs.getString("topic"),
                        LinkEventType.valueOf(rs.getString("event_type")),
                        rs.getString("payload")
                ),
                batchSize
        );
    }

    void markAsProcessedById(String eventId) {
        jdbcTemplate.update("UPDATE outbox_events SET processed = true WHERE id = ?::uuid", eventId);
    }

    void markAsProcessed(String aggregateId, LinkEventType eventType) {
        jdbcTemplate.update(
                "UPDATE outbox_events SET processed = true WHERE aggregate_id = ? AND event_type = ? AND processed = false",
                aggregateId, eventType.name()
        );
    }

    record OutboxRecord(String id, String aggregateId, String topic, LinkEventType eventType, String payload) {}
}