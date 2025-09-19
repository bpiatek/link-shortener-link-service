package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class LinkFacadeTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    LinkFacade linkFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LinkFixtures linkFixtures;

    @AfterEach
    void cleanup() {
        // Clean up the database after each test to ensure isolation.
        jdbcTemplate.update("DELETE FROM links");
    }

    @Test
    @DisplayName("should create and save a link when a valid custom short url is provided")
    void shouldCreateLinkWithCustomShortUrl() {
        // given
        var customShortUrl = "test-url";

        // when
        var response = linkFacade.createLink(USER_ID, LONG_URL, customShortUrl);

        // then:
        var count = linkFixtures.linksCountByShortUrl(customShortUrl);
        assertSoftly(s -> {
            s.assertThat(response).isNotNull();
            s.assertThat(response.shortUrl()).endsWith(customShortUrl);
            s.assertThat(response.longUrl()).isEqualTo(LONG_URL);
            s.assertThat(count).isEqualTo(1);
        });

    }

    @Test
    @DisplayName("should throw an exception when trying to use a custom code that already exists")
    void shouldFailWhenCustomCodeIsDuplicate() {
        // given
        var customShortUrl = "test-url";
        linkFixtures.aLink(TestLink.builder()
                .userId(USER_ID)
                .longUrl(LONG_URL)
                .shortUrl(customShortUrl)
                .build());

        // then
        assertThatThrownBy(() -> linkFacade.createLink(USER_ID, LONG_URL, customShortUrl))
                .isInstanceOf(ShortCodeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("should create and save a link with a random short url when no custom short url is provided")
    void shouldCreateLinkWithRandomCode() {
        // when
        var response = linkFacade.createLink(USER_ID, LONG_URL, null);

        // then
        var count = linkFixtures.linksCountByUserId(USER_ID);
        assertSoftly(s -> {
            s.assertThat(response).isNotNull();
            s.assertThat(response.shortUrl()).isNotBlank();
            s.assertThat(count).isEqualTo(1);
        });
    }

}