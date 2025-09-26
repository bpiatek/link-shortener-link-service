package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.jdbc.core.RowMapper;
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

        var params = new HashMap<String, Object>();
        params.put("user_id", link.userId());
        params.put("short_url", link.shortUrl());
        params.put("long_url", link.longUrl());
        params.put("title", link.title());
        params.put("notes", link.notes());
        params.put("is_active", link.isActive());
        params.put("created_at", providedDateOr(link.createdAt(), now));
        params.put("updated_at", providedDateOr(link.updatedAt(), now));
        params.put("expires_at", providedDateOr(link.expiresAt(), now.plus(7, DAYS)));

        var key = linkInsert.executeAndReturnKey(params);
        long generatedId = key.longValue();

        return link.withId(generatedId);
    }

    @Override
    public Optional<Link> findByShortUrl(String shortUrl) {
        var sql = """
                SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.created_at, l.updated_at, l.expires_at
                FROM links l
                WHERE l.short_url = :shortUrl""";

        var result = namedJdbcTemplate.query(sql, Map.of("shortUrl", shortUrl), LINK_ROW_MAPPER);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public List<Link> findByUserId(String userId) {
        var sql = """
                 SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.created_at, l.updated_at, l.expires_at
                 FROM links l
                 WHERE l.user_id = :userId""";

        return namedJdbcTemplate.query(sql, Map.of("userId", userId), LINK_ROW_MAPPER);
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
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            Optional.ofNullable(rs.getTimestamp("expires_at")).map(Timestamp::toInstant).orElse(null)
    );
}
