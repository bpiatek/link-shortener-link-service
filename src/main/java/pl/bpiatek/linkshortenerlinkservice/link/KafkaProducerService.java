package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import static java.nio.charset.StandardCharsets.UTF_8;

class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";
    private static final ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    public KafkaProducerService(String topicName,
                         KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    void sendLinkCreatedEvent(Link link) {
        var createdPayload = LinkCreated.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setLongUrl(link.longUrl())
                .setIsActive(link.isActive())
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(link.createdAt().getEpochSecond())
                        .setNanos(link.createdAt().getNano()).build())
                .setTitle(link.title() == null ? "" : link.title())
                .build();

        var eventToSend = LinkLifecycleEvent.newBuilder()
                .setLinkCreated(createdPayload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, String.valueOf(link.id()), eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        var snapshot = snapshotFactory.captureAll();

        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            try (var scope = snapshot.setThreadLocals()) {
                if (ex == null) {
                    log.info("Successfully published LinkCreated event for link ID: {} to partition: {} offset: {}",
                            link.id(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish LinkCreated event for link ID: {}. Reason: {}",
                            link.id(),
                            ex.getMessage(),
                            ex);
                }
            }
        });
    }
}