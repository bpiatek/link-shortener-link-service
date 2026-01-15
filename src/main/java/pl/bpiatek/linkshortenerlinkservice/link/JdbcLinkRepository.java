package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

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

    JdbcLinkRepository(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.linkInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("links")
                .usingGeneratedKeyColumns("id");
        this.clock = clock;
    }

    @Override
    public Link save(Link link) {
        var now = clock.instant();
        var createdAt = providedDateOr(link.createdAt(), now);

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

        var key = linkInsert.executeAndReturnKey(params);
        long generatedId = key.longValue();

        return link.withIdAndCreatedAt(generatedId, createdAt.toInstant());
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
                .addValue("updatedAt", Timestamp.from(clock.instant()));

        namedJdbcTemplate.update(sql, params);
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
    public void deleteByIdAndUserId(Long id, String userId) {
        var sql = "DELETE FROM links WHERE id = :id AND user_id = :userId";
        namedJdbcTemplate.update(sql, Map.of("id", id, "userId", userId));
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

    private Timestamp providedDateOr(Instant provided, Instant or) {
        return provided != null
                ? Timestamp.from(provided)
                : Timestamp.from(or);
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
