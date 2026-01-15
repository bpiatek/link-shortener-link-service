package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;

@Component
@ActiveProfiles("test")
class LinkFixtures {

    private final Clock clock;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final SimpleJdbcInsert linkInsert;

    LinkFixtures(Clock clock, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.clock = clock;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.linkInsert = new SimpleJdbcInsert(namedJdbcTemplate.getJdbcTemplate())
                .withTableName("links")
                .usingGeneratedKeyColumns("id");
    }

    Link aLink(TestLink link) {
        var now = clock.instant();

        Map<String, Object> params = Map.of(
                "user_id", link.getUserId(),
                "short_url", link.getShortUrl(),
                "long_url", link.getLongUrl(),
                "title", link.getTitle(),
                "notes", link.getNotes(),
                "is_active", link.isActive(),
                "is_custom", link.isCustom(),
                "created_at", getProvidedDateOr(link.getCreatedAt(), now),
                "updated_at", getProvidedDateOr(link.getUpdatedAt(), now),
                "expires_at", getProvidedDateOr(link.getExpiredAt(), now.plus(7, DAYS))
        );

        var key = linkInsert.executeAndReturnKey(params);
        long generatedId = key.longValue();

        return getLinkById(generatedId);
    }

    Link aLink() {
        return aLink(TestLink.builder().build());
    }

    Integer linksCountByShortUrl(String shortUrl) {
        return namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM links l WHERE l.short_url = :shortUrl",
                Map.of("shortUrl", shortUrl),
                Integer.class);
    }

    Integer linksCountByUserId(String userId) {
        return namedJdbcTemplate.queryForObject("SELECT COUNT(*) FROM links WHERE user_id = :userId", Map.of("userId", userId), Integer.class);
    }

    Link getLinkByShortUrl(String shortUrl) {
        var sql = """
                SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.is_custom, l.created_at, l.updated_at, l.expires_at
                FROM links l
                WHERE l.short_url = :short_url""";


        var result = namedJdbcTemplate.query(sql, Map.of("short_url", shortUrl), LINK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    Link getLinkById(Long id) {
        var sql = """
                SELECT l.id, l.user_id, l.short_url, l.long_url, l.title, l.notes, l.is_active, l.is_custom, l.created_at, l.updated_at, l.expires_at
                FROM links l
                WHERE l.id = :id""";


        var result = namedJdbcTemplate.query(sql, Map.of("id", id), LINK_ROW_MAPPER);

        return result.isEmpty() ? null : result.getFirst();
    }

    private Timestamp getProvidedDateOr(LocalDateTime provided, Instant or) {
        return provided != null
                ? Timestamp.from(provided.toInstant(ZoneOffset.UTC))
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
