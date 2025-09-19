package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class LinkFacadeKafkaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RedpandaContainer redpandaContainer =
            new RedpandaContainer(DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v24.1.4"));

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
    void shouldPersistToDatabaseAndPublishKafkaEvent() throws InterruptedException {
        // given
        var userId = "user-123";
        var longUrl = "https://example.com/full-integration-test";
        var customCode = "integ-test";

        // when
        var response = linkFacade.createLink(userId, longUrl, customCode);
        var record = testConsumer.awaitRecord(10, TimeUnit.SECONDS);

        // then
        assertSoftly(softly -> {
            // 1. Verify the database was updated correctly.
            Integer dbCount = linkFixtures.linksCountByShortUrl(customCode);
            softly.assertThat(dbCount).as("Database link count").isEqualTo(1);

            // 2. Verify the Kafka message is not null.
            softly.assertThat(record).as("Consumed Kafka record").isNotNull();

            // 3. Verify the Kafka message key.
            // We need to get the ID from the DB to confirm the key is correct.
            var linkFromDb = linkFixtures.getLinkByShortUrl(customCode);
            softly.assertThat(record.key()).as("Kafka message key").isEqualTo(String.valueOf(linkFromDb.id()));

            // 4. Verify the Kafka message payload (the Protobuf object).
            var message = record.value();
            softly.assertThat(message.hasLinkCreated()).as("Event is of type LinkCreated").isTrue();
            var createdPayload = message.getLinkCreated();
            softly.assertThat(createdPayload.getLinkId()).isEqualTo(String.valueOf(linkFromDb.id()));
            softly.assertThat(createdPayload.getUserId()).isEqualTo(userId);
            softly.assertThat(createdPayload.getShortUrl()).isEqualTo(customCode);
            softly.assertThat(createdPayload.getLongUrl()).isEqualTo(longUrl);

            // 5. Verify the Kafka message headers.
            var headers = record.headers();
            softly.assertThat(new String(headers.lastHeader("source").value(), StandardCharsets.UTF_8))
                    .as("Kafka 'source' header").isEqualTo("link-service");
            softly.assertThat(headers.lastHeader("trace-id").value()).as("Kafka 'trace-id' header").isNotNull();
        });
    }
}
