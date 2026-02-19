package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.CompletionException;

class LinkLifecycleKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LinkLifecycleKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate;
    private final Clock clock;

    public LinkLifecycleKafkaProducer(String topicName,
                                      KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate,
                                      Clock clock) {
        this.topicName = topicName;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    void sendLifecycleEvent(Link link, LinkEventType eventType) {
        var eventToSend = buildLifecycleEvent(link, eventType);
        var linkIdStr = String.valueOf(link.id());

        var producerRecord = new ProducerRecord<>(topicName, linkIdStr, eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(StandardCharsets.UTF_8)));

        try {
            // Wait for Kafka ACK before allowing the Listener to mark the Outbox DB row as processed
            var result = kafkaTemplate.send(producerRecord).join();

            log.info("Successfully published {} event for link ID: {} to partition: {} offset: {}",
                    eventType,
                    linkIdStr,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (CompletionException e) {
            var realCause = e.getCause() != null ? e.getCause() : e;

            log.error("Failed to publish {} event for link ID: {}. Reason: {}",
                    eventType, linkIdStr, realCause.getMessage(), realCause);

            // We MUST throw the exception so the Fast-Track listener knows it failed
            throw new RuntimeException("Kafka send failed for " + eventType, realCause);
        }
    }


    private LinkLifecycleEventProto.LinkLifecycleEvent buildLifecycleEvent(Link link, LinkEventType eventType) {
        var builder = LinkLifecycleEventProto.LinkLifecycleEvent.newBuilder();

        switch (eventType) {
            case LINK_CREATED -> builder.setLinkCreated(mapToCreated(link));
            case LINK_UPDATED -> builder.setLinkUpdated(mapToUpdated(link));
            case LINK_DELETED -> builder.setLinkDeleted(mapToDeleted(link));
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        return builder.build();
    }

    private LinkLifecycleEventProto.LinkCreated mapToCreated(Link link) {
        return LinkLifecycleEventProto.LinkCreated.newBuilder()
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
    }

    private LinkLifecycleEventProto.LinkUpdated mapToUpdated(Link link) {
        return LinkLifecycleEventProto.LinkUpdated.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setLongUrl(link.longUrl())
                .setIsActive(link.isActive())
                .setUpdatedAt(Timestamp.newBuilder()
                        .setSeconds(link.updatedAt().getEpochSecond())
                        .setNanos(link.updatedAt().getNano()).build())
                .setTitle(link.title() == null ? "" : link.title())
                .build();
    }

    private LinkLifecycleEventProto.LinkDeleted mapToDeleted(Link link) {
        // You mentioned the Deleted producer uses the current clock time for deletedAt
        var now = clock.instant();

        return LinkLifecycleEventProto.LinkDeleted.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setDeletedAt(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano()).build())
                .build();
    }
}