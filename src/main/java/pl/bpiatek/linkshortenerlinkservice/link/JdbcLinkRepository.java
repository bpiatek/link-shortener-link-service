package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;

class JdbcLinkRepository implements LinkRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final SimpleJdbcInsert linkInsert;
    private final Clock clock;
    private final OutboxRepository outboxRepository;

    JdbcLinkRepository(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock, OutboxRepository outboxRepository) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.linkInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("links")
                .usingGeneratedKeyColumns("id");
        this.clock = clock;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link save(Link link) {
        var now = clock.instant();
        var createdAt = providedDateOr(link.createdAt(), now);
        var params = buildInsertParams(link, createdAt, now);

        var key = linkInsert.executeAndReturnKey(params);
        var savedLink = link.withIdAndCreatedAt(key.longValue(), createdAt.toInstant());

        outboxRepository.saveEvent(
                String.valueOf(savedLink.id()),
                LinkEventType.LINK_CREATED,
                savedLink
        );

        return savedLink;
    }

    private Map<String, Object> buildInsertParams(Link link, Timestamp createdAt, Instant now) {
        var params = new HashMap<String, Object>();
        params.put("user_id", link.userId());
        params.put("short_url", link.shortUrl());
        params.put("long_url", link.longUrl());
        params.put("title", link.title());
        params.put("notes", link.notes());
        params.put("is_active", link.isActive());
        params.put("is_custom", link.isCustom());
        params.put("created_at", createdAt);
        params.put("updated_at", providedDateOr(link.updatedAt(), now));
        params.put("expires_at", providedDateOr(link.expiresAt(), now.plus(7, DAYS)));
        return params;
    }

    @Override
    public Optional<Link> findByShortUrl(String shortUrl) {
        var sql = """
                SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.is_custom, l.created_at, l.updated_at, l.expires_at
                FROM links l
                WHERE l.short_url = :shortUrl""";

        var result = namedJdbcTemplate.query(sql, Map.of("shortUrl", shortUrl), LINK_ROW_MAPPER);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public List<Link> findByUserId(String userId) {
        var sql = """
                 SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.is_custom, l.created_at, l.updated_at, l.expires_at
                 FROM links l
                 WHERE l.user_id = :userId""";

        return namedJdbcTemplate.query(sql, Map.of("userId", userId), LINK_ROW_MAPPER);
    }

    @Override
    public void update(Link link) {
        var now = clock.instant();
        var updatedLink = link.withUpdatedAt(now);

        var sql = """
            UPDATE links
            SET long_url = :longUrl,
                title = :title,
                is_active = :isActive,
                updated_at = :updatedAt
            WHERE id = :id
            """;

        var params = new MapSqlParameterSource()
                .addValue("id", link.id())
                .addValue("longUrl", link.longUrl())
                .addValue("title", link.title())
                .addValue("isActive", link.isActive())
                .addValue("updatedAt", Timestamp.from(now));

        namedJdbcTemplate.update(sql, params);

        outboxRepository.saveEvent(
                String.valueOf(link.id()),
                LinkEventType.LINK_UPDATED,
                updatedLink
        );
    }

    @Override
    public Optional<Link> findByIdAndUserId(Long id, String userId) {
        var sql = """
                SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.is_custom, l.created_at, l.updated_at, l.expires_at
                FROM links l
                WHERE l.id = :id AND l.user_id = :userId""";

        var params = Map.of("id", id, "userId", userId);
        var result = namedJdbcTemplate.query(sql, params, LINK_ROW_MAPPER);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public void deleteByIdAndUserId(Link link) {
        var sql = "DELETE FROM links WHERE id = :id AND user_id = :userId";

        var params = new MapSqlParameterSource()
                .addValue("id", link.id())
                .addValue("userId", link.userId());

        int deletedRows = namedJdbcTemplate.update(sql, params);

        if (deletedRows > 0) {
            outboxRepository.saveEvent(
                    String.valueOf(link.id()),
                    LinkEventType.LINK_DELETED,
                    link);
        }
    }

    @Override
    public int deleteDeactivatedCustomLinksOlderThan(Instant cutoffDate) {
        var sql = """
            DELETE FROM links
            WHERE is_custom = true
              AND is_active = false
              AND updated_at < :cutoffDate
            """;
        
        var params = new MapSqlParameterSource()
                .addValue("cutoffDate", Timestamp.from(cutoffDate));
        
        return namedJdbcTemplate.update(sql, params);
    }

    private Timestamp providedDateOr(Instant provided, Instant fallback) {
        return provided != null
                ? Timestamp.from(provided)
                : Timestamp.from(fallback);
    }

    private static final RowMapper<Link> LINK_ROW_MAPPER = (rs, rowNum) -> new Link(
            rs.getLong("id"),
            rs.getString("user_id"),
            rs.getString("short_url"),
            rs.getString("long_url"),
            rs.getString("title"),
            rs.getString("notes"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_custom"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            Optional.ofNullable(rs.getTimestamp("expires_at")).map(Timestamp::toInstant).orElse(null)
    );
}
