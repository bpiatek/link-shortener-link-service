package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkCreated;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.concurrent.CompletionException;

import static java.nio.charset.StandardCharsets.UTF_8;

class LinkCreatedKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LinkCreatedKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    public LinkCreatedKafkaProducer(String topicName,
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

        try {
            var result = kafkaTemplate.send(producerRecord).join();

            log.info("Successfully published LinkCreated event for link ID: {} to partition: {} offset: {}",
                    link.id(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (CompletionException e) {
            var realCause = e.getCause() != null ? e.getCause() : e;

            log.error("Failed to publish LinkCreated event for link ID: {}. Reason: {}",
                    link.id(), e.getMessage(), realCause);

            throw new RuntimeException("Kafka send failed", realCause);
        }
    }
}