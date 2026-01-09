package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;

import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LinkFacadeKafkaIT extends IntegrationTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private LinkFixtures linkFixtures;

    @Test
    void shouldPersistLinkWithCustomStrategyAndPublishKafkaEvent() throws InterruptedException {
        // given
        var customCode = "integ-test";

        // when
        linkFacade.createLink(USER_ID, LONG_URL, customCode, true, "title");

        // then
        var record = testLinkLifecycleEventConsumer.awaitRecord(10, TimeUnit.SECONDS);
        assertSoftly(softly -> {
            var linksCountByShortUrl = linkFixtures.linksCountByShortUrl(customCode);
            softly.assertThat(linksCountByShortUrl).isOne();

            softly.assertThat(record).isNotNull();
            var linkFromDb = linkFixtures.getLinkByShortUrl(customCode);
            softly.assertThat(record.key()).isEqualTo(linkFromDb.id().toString());

            var message = record.value();
            softly.assertThat(message.hasLinkCreated()).isTrue();
            var createdPayload = message.getLinkCreated();
            softly.assertThat(createdPayload.getLinkId()).isEqualTo(linkFromDb.id().toString());
            softly.assertThat(createdPayload.getUserId()).isEqualTo(USER_ID);
            softly.assertThat(createdPayload.getShortUrl()).isEqualTo(customCode);
            softly.assertThat(createdPayload.getLongUrl()).isEqualTo(LONG_URL);
            softly.assertThat(createdPayload.getIsActive()).isTrue();
            softly.assertThat(createdPayload.getCreatedAt()).isEqualTo(Timestamp.newBuilder()
                    .setSeconds(linkFromDb.createdAt().getEpochSecond())
                    .setNanos(linkFromDb.createdAt().getNano())
                    .build());
            softly.assertThat(createdPayload.getTitle()).isEqualTo("title");

            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), UTF_8)).isEqualTo("link-service");
        });
    }

    @Test
    void shouldPersistLinkWithRandomStrategyAndPublishKafkaEvent() throws InterruptedException {
        // when
        linkFacade.createLink(USER_ID, LONG_URL, null, true, "title");

        // then
        var record = testLinkLifecycleEventConsumer.awaitRecord(5, TimeUnit.SECONDS);
        assertSoftly(softly -> {
            softly.assertThat(record).isNotNull();
            softly.assertThat(record.key()).isNotBlank();

            var message = record.value();
            softly.assertThat(message.hasLinkCreated()).isTrue();
            var createdPayload = message.getLinkCreated();
            softly.assertThat(createdPayload.getLinkId()).isNotBlank();
            softly.assertThat(createdPayload.getUserId()).isEqualTo(USER_ID);
            softly.assertThat(createdPayload.getShortUrl()).isNotBlank();
            softly.assertThat(createdPayload.getLongUrl()).isEqualTo(LONG_URL);
            softly.assertThat(createdPayload.getIsActive()).isTrue();
            softly.assertThat(createdPayload.getTitle()).isEqualTo("title");
            softly.assertThat(createdPayload.getCreatedAt()).isNotNull();


            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), UTF_8)).isEqualTo("link-service");
        });
    }

    @Test
    void shouldPersistLinkWithRandomStrategyAndPublishKafkaEventWhenTitleIsNotPresent() throws InterruptedException {
        // when
        linkFacade.createLink(USER_ID, LONG_URL, null, true, null);

        // then
        var record = testLinkLifecycleEventConsumer.awaitRecord(10, TimeUnit.SECONDS);
        assertSoftly(softly -> {
            softly.assertThat(record).isNotNull();
            softly.assertThat(record.key()).isNotBlank();

            var message = record.value();
            softly.assertThat(message.hasLinkCreated()).isTrue();
            var createdPayload = message.getLinkCreated();
            softly.assertThat(createdPayload.getLinkId()).isNotBlank();
            softly.assertThat(createdPayload.getUserId()).isEqualTo(USER_ID);
            softly.assertThat(createdPayload.getShortUrl()).isNotBlank();
            softly.assertThat(createdPayload.getLongUrl()).isEqualTo(LONG_URL);
            softly.assertThat(createdPayload.getIsActive()).isTrue();
            softly.assertThat(createdPayload.getTitle()).isEmpty();
            softly.assertThat(createdPayload.getCreatedAt()).isNotNull();

            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), UTF_8)).isEqualTo("link-service");
        });
    }
}
