package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent.EventPayloadCase.LINK_CREATED;

class LinkFacadeCreateLinkIT extends IntegrationTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";
    private static final String TITLE = "title";

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private LinkFixtures linkFixtures;

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
    void shouldSendEventWhenLinkIsCreated() throws InterruptedException {
        // given
        var customShortUrl = "test-url";

        // when
        linkFacade.createLink(USER_ID, LONG_URL, customShortUrl, true, "test-title");

        // then
        var record = testLinkLifecycleEventConsumer.awaitRecord(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        var link = linkFixtures.getLinkByShortUrl(customShortUrl);
        assertThat(link).isNotNull();

        assertSoftly(s -> {
            var envelope = record.value();
            s.assertThat(envelope.getEventPayloadCase()).isEqualTo(LINK_CREATED);

            var message = envelope.getLinkCreated();
            s.assertThat(message.getLinkId()).isEqualTo(String.valueOf(link.id()));
            s.assertThat(message.getShortUrl()).isEqualTo(link.shortUrl());
            s.assertThat(message.getTitle()).isEqualTo(link.title());
            s.assertThat(message.getCreatedAt().getNanos()).isEqualTo(link.createdAt().getNano());
            s.assertThat(message.getIsActive()).isEqualTo(link.isActive());
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