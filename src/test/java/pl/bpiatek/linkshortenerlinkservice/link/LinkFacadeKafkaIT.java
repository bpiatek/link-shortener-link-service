package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.bpiatek.linkshortenerlinkservice.config.WithFullInfrastructure;

import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest
@ActiveProfiles("test")
class LinkFacadeKafkaIT implements WithFullInfrastructure {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer");
        registry.add("spring.kafka.consumer.properties.specific.protobuf.value.type",
                () -> "pl.bpiatek.contracts.link.LinkLifecycleEventProto$LinkLifecycleEvent");
    }

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestLinkEventConsumer testConsumer;

    @Autowired
    private LinkFixtures linkFixtures;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM links");
        testConsumer.reset();
    }

    @Test
    void shouldPersistLinkWithCustomStrategyAndPublishKafkaEvent() throws InterruptedException {
        // given
        var customCode = "integ-test";

        // when
        linkFacade.createLink(USER_ID, LONG_URL, customCode, true, "title");

        // then
        var record = testConsumer.awaitRecord(10, TimeUnit.SECONDS);
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
            softly.assertThat(createdPayload.getTitle()).isEqualTo("title");

            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), UTF_8)).isEqualTo("link-service");
            softly.assertThat(headers.lastHeader("trace-id").value()).isNotNull();
        });
    }

    @Test
    void shouldPersistLinkWithRandomStrategyAndPublishKafkaEvent() throws InterruptedException {
        // when
        linkFacade.createLink(USER_ID, LONG_URL, null, true, "title");

        // then
        var record = testConsumer.awaitRecord(10, TimeUnit.SECONDS);
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

            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), UTF_8)).isEqualTo("link-service");
            softly.assertThat(headers.lastHeader("trace-id").value()).isNotNull();
        });
    }
}
