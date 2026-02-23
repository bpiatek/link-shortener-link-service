package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Transactional
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
                UTC));

        var linkToSave = new Link(
                null, "123", "aB5xZ1", "https://example.com/a-very-long-url",
                "Example Title", "Some notes", true, false,
                now, now, now.plus(7, DAYS)
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
            s.assertThat(savedLink.isCustom()).isEqualTo(link.isCustom());
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

    @Test
    void shouldFindByIdAndUserId() {
        // given
        var link = linkFixtures.aLink(TestLink.builder().userId("user-1").build());

        // when
        var foundLink = linkRepository.findByIdAndUserId(link.id(), "user-1");

        // then
        assertThat(foundLink).isPresent();
        assertThat(foundLink.get().id()).isEqualTo(link.id());
    }

    @Test
    void shouldNotFindByIdAndWrongUserId() {
        // given
        var link = linkFixtures.aLink(TestLink.builder().userId("user-1").build());

        // when
        var foundLink = linkRepository.findByIdAndUserId(link.id(), "wrong-user");

        // then
        assertThat(foundLink).isNotPresent();
    }

    @Test
    void shouldUpdateLink() {
        // given
        var originalLink = linkFixtures.aLink(TestLink.builder()
                .title("Old Title")
                .longUrl("https://old.com")
                .isActive(true)
                .build());

        var expectedUpdateTime = DEFAULT_NOW.plus(5, ChronoUnit.DAYS);
        setCurrentTime(expectedUpdateTime);

        var linkToUpdate = new Link(
                originalLink.id(), originalLink.userId(), originalLink.shortUrl(),
                "https://new.com", "New Title", originalLink.notes(), false,
                originalLink.isCustom(), originalLink.createdAt(), expectedUpdateTime, originalLink.expiresAt()
        );

        // when
        linkRepository.update(linkToUpdate);

        // then
        var fetchedLink = getLinkWithId(originalLink.id());
        assertSoftly(s -> {
            s.assertThat(fetchedLink.title()).isEqualTo("New Title");
            s.assertThat(fetchedLink.longUrl()).isEqualTo("https://new.com");
            s.assertThat(fetchedLink.isActive()).isFalse();
            s.assertThat(fetchedLink.updatedAt()).isEqualTo(expectedUpdateTime);
        });
    }

    @Test
    void shouldDeleteByIdAndUserId() {
        // given
        var link = linkFixtures.aLink(TestLink.builder().userId("user-1").build());

        // when
        linkRepository.deleteByIdAndUserId(link.id(), link.userId());

        // then
        var foundLink = linkRepository.findByIdAndUserId(link.id(), link.userId());
        assertThat(foundLink).isNotPresent();
    }

    @Test
    void shouldDeleteAndReturnDeactivatedCustomLinksOlderThanCutoff() {
        // given
        var cutoffDate = LocalDateTime.now().minusDays(30);
        var veryOldDate = LocalDateTime.now().minusDays(60);
        var recentDate = LocalDateTime.now().minusDays(10);

        // SHOULD BE DELETED
        var targetLink = linkFixtures.aLink(TestLink.builder()
                .shortUrl("abc").isCustom(true).isActive(false).updatedAt(veryOldDate).build());

        // SHOULD STAY
        var recentInactiveLink = linkFixtures.aLink(TestLink.builder()
                .shortUrl("cab").isCustom(true).isActive(false).updatedAt(recentDate).build());
        var activeOldLink = linkFixtures.aLink(TestLink.builder()
                .shortUrl("bac").isCustom(true).isActive(true).updatedAt(veryOldDate).build());
        var randomOldLink = linkFixtures.aLink(TestLink.builder()
                .shortUrl("e38").isCustom(false).isActive(false).updatedAt(veryOldDate).build());

        // when
        var deletedLinks = linkRepository.deleteAndReturnDeactivatedCustomLinksOlderThan(cutoffDate.toInstant(UTC));

        // then
        assertThat(deletedLinks).hasSize(1);
        assertThat(deletedLinks.getFirst().id()).isEqualTo(targetLink.id());

        assertThat(linkRepository.findByIdAndUserId(targetLink.id(), targetLink.userId())).isNotPresent();

        assertThat(linkRepository.findByIdAndUserId(recentInactiveLink.id(), recentInactiveLink.userId())).isPresent();
        assertThat(linkRepository.findByIdAndUserId(activeOldLink.id(), activeOldLink.userId())).isPresent();
        assertThat(linkRepository.findByIdAndUserId(randomOldLink.id(), randomOldLink.userId())).isPresent();
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
                    is_custom,
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
                rs.getBoolean("is_custom"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                Optional.ofNullable(rs.getTimestamp("expires_at")).map(Timestamp::toInstant).orElse(null)
        ), id);
    }
}