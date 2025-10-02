package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pl.bpiatek.linkshortenerlinkservice.config.WithFullInfrastructure;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ActiveProfiles("test")
@SpringBootTest
class LinkFacadeTest implements WithFullInfrastructure {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";
    private static final String TITLE = "title";

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LinkFixtures linkFixtures;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM links");
    }

    @Test
    void shouldCreateLinkWithCustomShortUrl() {
        // given
        var customShortUrl = "test-url";

        // when
        var response = linkFacade.createLink(USER_ID, LONG_URL, customShortUrl, true, TITLE);

        // then:
        var link = linkFixtures.getLinkByShortUrl(customShortUrl);
        assertSoftly(s -> {
            s.assertThat(response).isNotNull();
            s.assertThat(response.shortUrl()).endsWith(customShortUrl);
            s.assertThat(response.longUrl()).isEqualTo(LONG_URL);
            s.assertThat(link.title()).isEqualTo(TITLE);
        });

    }

    @Test
    void shouldFailWhenCustomShortUrlIsDuplicate() {
        // given
        var customShortUrl = "test-url";
        linkFixtures.aLink(TestLink.builder()
                .userId(USER_ID)
                .longUrl(LONG_URL)
                .shortUrl(customShortUrl)
                .build());

        // then
        assertThatThrownBy(() -> linkFacade.createLink(USER_ID, LONG_URL, customShortUrl, true, null))
                .isInstanceOf(ShortCodeAlreadyExistsException.class);
    }

    @Test
    void shouldCreateLinkWithRandomShortUrl() {
        // when
        var response = linkFacade.createLink(USER_ID, LONG_URL, null, false, null);

        // then
        var count = linkFixtures.linksCountByUserId(USER_ID);
        assertSoftly(s -> {
            s.assertThat(response).isNotNull();
            s.assertThat(response.shortUrl()).isNotBlank();
            s.assertThat(count).isEqualTo(1);
        });
    }

}