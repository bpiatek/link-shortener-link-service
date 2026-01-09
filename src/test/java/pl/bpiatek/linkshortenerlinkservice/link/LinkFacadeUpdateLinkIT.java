package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent.EventPayloadCase.LINK_UPDATED;

class LinkFacadeUpdateLinkIT extends IntegrationTest {

    private static final String USER_ID = "123";

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private LinkFixtures linkFixtures;

    @Test
    void shouldUpdateLink() {
        // given
        var newLongUrl = "test.com/looooooooong";
        var newTestTitle = "new test title";
        var link = linkFixtures.aLink();
        var request = new UpdateLinkRequest(newLongUrl, true, newTestTitle);

        // when
        var updatedLink = linkFacade.updateLink(USER_ID, link.id(), request);

        // then
        var linkFromDb = linkFixtures.getLinkById(Long.valueOf(updatedLink.id()));
        assertThat(linkFromDb).isNotNull();

        assertSoftly(s -> {
           s.assertThat(updatedLink.id()).isEqualTo(String.valueOf(link.id()));
           s.assertThat(updatedLink.title()).isEqualTo(newTestTitle);
           s.assertThat(updatedLink.longUrl()).contains(newLongUrl);
           s.assertThat(updatedLink.userId()).isEqualTo(USER_ID);
           s.assertThat(updatedLink.isActive()).isTrue();
           s.assertThat(updatedLink.updatedAt()).isNotNull();
        });
    }

    @Test
    void shouldSendEventWhenLinkIsUpdated() throws InterruptedException {
        // given
        var newLongUrl = "test.com/looooooooong";
        var newTestTitle = "new test title";
        var link = linkFixtures.aLink();
        var request = new UpdateLinkRequest(newLongUrl, true, newTestTitle);

        // when
        var updatedLink = linkFacade.updateLink(USER_ID, link.id(), request);

        // then
        var record = testLinkLifecycleEventConsumer.awaitRecord(5, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        assertSoftly(s -> {
            var envelope = record.value();
            s.assertThat(envelope.getEventPayloadCase()).isEqualTo(LINK_UPDATED);

            var message = envelope.getLinkUpdated();
            s.assertThat(message.getLinkId()).isEqualTo(updatedLink.id());
            s.assertThat(message.getShortUrl()).isEqualTo(updatedLink.shortUrl());
            s.assertThat(message.getLongUrl()).isEqualTo(updatedLink.longUrl());
            s.assertThat(message.getIsActive()).isEqualTo(updatedLink.isActive());
            s.assertThat(message.getTitle()).isEqualTo(updatedLink.title());
            s.assertThat(message.getUpdatedAt()).isNotNull();
        });
    }
}
