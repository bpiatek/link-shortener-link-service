package pl.bpiatek.linkshortenerlinkservice.link;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";
    private static final String METRIC_NAME = "link_service_kafka_messages_total";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public KafkaProducerService(String topicName,
                         KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate,
                         MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.topicName = topicName;
    }

    void sendLinkCreatedEvent(Link link) {
        var createdPayload = LinkCreated.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setLongUrl(link.longUrl())
                .setIsActive(link.isActive())
                .setTitle(link.title())
                .build();

        var eventToSend = LinkLifecycleEvent.newBuilder()
                .setLinkCreated(createdPayload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, String.valueOf(link.id()), eventToSend);
        producerRecord.headers().add(new RecordHeader("trace-id", UUID.randomUUID().toString().getBytes(UTF_8)));
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published LinkCreated event for link ID: {}", link.id());
                meterRegistry.counter(METRIC_NAME,
                        "topic", topicName,
                        "status", "success"
                ).increment();
            } else {
                log.error("Failed to publish LinkCreated event for link ID: {}. Reason: {}", link.id(), ex.getMessage());
                var cause = ex.getCause();
                meterRegistry.counter(METRIC_NAME,
                        "topic", topicName,
                        "status", "failure",
                        "exception", cause != null ? cause.getClass().getSimpleName() : ex.getClass().getSimpleName()
                ).increment();
            }
        });
    }
}