package pl.bpiatek.linkshortenerlinkservice.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.List;
import java.util.UUID;

class OutboxRepository {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepository.class);

    private final NamedParameterJdbcTemplate namedJdbcTemplate;


    OutboxRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.namedJdbcTemplate = jdbcTemplate;
    }

    void saveEvent(String aggregateId, LinkEventType eventType, LinkLifecycleEvent protobufEvent) {
        var eventId = UUID.fromString(protobufEvent.getEventId());
        var params = new MapSqlParameterSource()
                .addValue("id", eventId)
                .addValue("aggregate_id", aggregateId)
                .addValue("topic", "link-lifecycle-events")
                .addValue("event_type", eventType.name())
                .addValue("payload", protobufEvent.toByteArray());

        var sql = """
                INSERT INTO outbox_events (id, aggregate_id, topic, event_type, payload, processed, created_at)
                VALUES (:id, :aggregate_id, :topic, :event_type, :payload, false, NOW())
                """;
        namedJdbcTemplate.update(sql, params);
    }

    List<OutboxRecord> fetchStalledEvents(int batchSize) {
        var sql = """
                SELECT id, aggregate_id, topic, event_type, payload
                FROM outbox_events
                WHERE processed = false
                  AND created_at < NOW() - INTERVAL '10 seconds'
                LIMIT :batchSize FOR UPDATE SKIP LOCKED
                """;

        var params = new MapSqlParameterSource("batchSize", batchSize);

        return namedJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new OutboxRecord(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("aggregate_id"),
                        rs.getString("topic"),
                        LinkEventType.valueOf(rs.getString("event_type")),
                        rs.getBytes("payload")
                )
        );
    }

    void saveEventsInBatch(SqlParameterSource[] batchArgs) {
        var sql = """
                INSERT INTO outbox_events
                (id, aggregate_id, topic, event_type, payload, processed, created_at) 
                VALUES (:id, :aggregate_id, :topic, :event_type, :payload, false, NOW())
                """;

        namedJdbcTemplate.batchUpdate(sql, batchArgs);
    }

    void markAsProcessedById(UUID eventId) {
        var sql = "UPDATE outbox_events SET processed = true WHERE id = :id";
        var params = new MapSqlParameterSource("id", eventId);

        namedJdbcTemplate.update(sql, params);
    }

    void markAsProcessedInBatch(List<UUID> eventIds) {
        var sql = "UPDATE outbox_events SET processed = true WHERE id = :id";

        var batchArgs = eventIds.stream()
                .map(id -> new MapSqlParameterSource("id", id))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(sql, batchArgs);
    }

    record OutboxRecord(UUID id, String aggregateId, String topic, LinkEventType eventType, byte[] payload) {
    }
}