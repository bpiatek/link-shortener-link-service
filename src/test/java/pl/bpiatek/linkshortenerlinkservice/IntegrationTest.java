package pl.bpiatek.linkshortenerlinkservice;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.time.Instant;
import java.time.ZoneId;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IntegrationTest.TestConfig.class)
@RecordApplicationEvents
public abstract class IntegrationTest {

    @ServiceConnection
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    public static final RedpandaContainer redpanda = new RedpandaContainer(
            DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v24.1.4")
    );

    static {
        postgres.start();
        redpanda.start();
    }

    public static final Instant DEFAULT_NOW = Instant.parse("2025-11-01T12:00:00Z");

    @Autowired
    private MutableClock mutableClock;

    protected void setCurrentTime(Instant newTime) {
        mutableClock.setInstant(newTime);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ApplicationEvents events;

    @Autowired(required = false)
    protected TestKafkaConsumer<LinkLifecycleEvent> testLinkLifecycleEventConsumer;

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        var eventType = "pl.bpiatek.contracts.link.LinkLifecycleEventProto$LinkLifecycleEvent";

        registry.add("spring.kafka.producer.properties.specific.protobuf.value.type", () -> eventType);
        registry.add("spring.kafka.consumer.properties.specific.protobuf.value.type", () -> eventType);
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);

        registry.add("spring.kafka.producer.properties.auto.register.schemas", () -> "true");
        registry.add("spring.kafka.producer.properties.use.latest.version", () -> "false");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM links");

        mutableClock.setInstant(DEFAULT_NOW);

        if (testLinkLifecycleEventConsumer != null) {
            testLinkLifecycleEventConsumer.reset();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(DEFAULT_NOW, ZoneId.systemDefault());
        }

        @Bean
        TestKafkaConsumer<LinkLifecycleEvent> testLinkEventConsumer() {
            return new TestKafkaConsumer<>();
        }

        @KafkaListener(
                topics = "${topic.link.lifecycle}",
                groupId = "integration-test-shared-group"
        )
        void listen(ConsumerRecord<String, LinkLifecycleEvent> record) {
            testLinkEventConsumer().handle(record);
        }

        @Bean
        public NewTopic linkLifecycleTopic(@Value("${topic.link.lifecycle}") String topicName) {
            return TopicBuilder.name(topicName)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }

        @Bean
        public ApplicationRunner waitForKafkaAssignment(KafkaListenerEndpointRegistry registry) {
            return args ->
                    registry.getListenerContainers().forEach(container ->
                            ContainerTestUtils.waitForAssignment(container, 1));
        }
    }
}
