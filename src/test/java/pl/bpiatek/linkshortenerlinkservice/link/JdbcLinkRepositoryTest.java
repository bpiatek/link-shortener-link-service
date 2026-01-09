package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class JdbcLinkRepositoryTest extends IntegrationTest {

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    LinkFixtures linkFixtures;

    @Test
    void shouldSaveValidLink() {
        // given
        var now = Instant.now(Clock.fixed(
                Instant.parse("2025-08-22T10:00:00Z"),
                ZoneOffset.UTC));

        var linkToSave = new Link(
                null,
                "123",
                "aB5xZ1",
                "https://example.com/a-very-long-url",
                "Example Title",
                "Some notes",
                true,
                now,
                now,
                now.plus(7, DAYS)
        );

        // when
        var savedLink = linkRepository.save(linkToSave);

        // then
        assertThat(savedLink.id()).isNotNull();
        var link = getLinkWithId(savedLink.id());
        assertSoftly(s -> {
            s.assertThat(savedLink.id()).isEqualTo(link.id());
            s.assertThat(savedLink.userId()).isEqualTo(link.userId());
            s.assertThat(savedLink.shortUrl()).isEqualTo(link.shortUrl());
            s.assertThat(savedLink.longUrl()).isEqualTo(link.longUrl());
            s.assertThat(savedLink.title()).isEqualTo(link.title());
            s.assertThat(savedLink.notes()).isEqualTo(link.notes());
            s.assertThat(savedLink.isActive()).isEqualTo(link.isActive());
            s.assertThat(savedLink.createdAt()).isEqualTo(link.createdAt());
            s.assertThat(savedLink.updatedAt()).isEqualTo(link.updatedAt());
            s.assertThat(savedLink.expiresAt()).isEqualTo(link.expiresAt());
        });
    }

    @Test
    void shouldFindLinkByShortUrl() {
        // given
        var link = linkFixtures.aLink(TestLink.builder()
                .shortUrl("edt2w")
                .longUrl("https://www.example.com/longlink")
                .build());

        // when
        var foundLink = linkRepository.findByShortUrl(link.shortUrl());

        // then
        assertThat(foundLink).isPresent();
        assertThat(foundLink.get()).isEqualTo(link);
    }

    @Test
    void shouldReturnEmptyWhenNoLinkFoundByShortUrl() {
        // given
        var nonExistentShortUrl = "nonExistentShortUrl";

        // when
        var foundLink = linkRepository.findByShortUrl(nonExistentShortUrl);

        // then
        assertThat(foundLink).isNotPresent();
    }

    @Test
    void shouldFindLinkByUserId() {
        // given
        var link = linkFixtures.aLink(TestLink.builder()
                .userId("1")
                .build());

        // when
        var foundLinks = linkRepository.findByUserId(link.userId());

        // then
        assertThat(foundLinks).hasSize(1);
        assertThat(foundLinks.getFirst()).isEqualTo(link);
    }

    @Test
    void shouldFindAllLinksByUserId() {
        // given
        var link1 = linkFixtures.aLink(TestLink.builder()
                .userId("1")
                .longUrl("https://www.example.com/longlink")
                .shortUrl("edt2w")
                .build());

        var link2 = linkFixtures.aLink(TestLink.builder()
                .userId("1")
                .longUrl("https://www.example.com/longlink2")
                .shortUrl("n2ut8")
                .build());

        // when
        var foundLinks = linkRepository.findByUserId(link1.userId());

        // then
        assertThat(foundLinks).hasSize(2);
        assertThat(getLinkWithId(foundLinks, link1.id())).isEqualTo(link1);
        assertThat(getLinkWithId(foundLinks, link2.id())).isEqualTo(link2);
    }

    private Link getLinkWithId(List<Link> links, Long linkId) {
        return links
                .stream().filter(link -> link.id().equals(linkId))
                .findFirst()
                .orElse(null);
    }

    @Test
    void shouldReturnEmptyListWhenNoLinkFoundByUserId() {
        // given
        var nonExistentUserId = "9999999";

        // when
        var foundLinks = linkRepository.findByUserId(nonExistentUserId);

        // then
        assertThat(foundLinks).isEmpty();
    }

    private Link getLinkWithId(Long id) {
        var sql = """
                SELECT
                    id,
                    user_id,
                    short_url,
                    long_url,
                    title,
                    notes,
                    is_active,
                    created_at,
                    updated_at,
                    expires_at
                FROM links
                WHERE id = ?""";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new Link(
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
        ), id);
    }
}