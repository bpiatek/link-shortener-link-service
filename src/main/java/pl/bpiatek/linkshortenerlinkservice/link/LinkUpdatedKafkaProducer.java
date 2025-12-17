package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import static java.nio.charset.StandardCharsets.UTF_8;

class LinkUpdatedKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LinkUpdatedKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate;

    public LinkUpdatedKafkaProducer(String topicName,
                                    KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    void sendLinkUpdatedEvent(Link link) {
        var updatedPayload = LinkLifecycleEventProto.LinkUpdated.newBuilder()
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

        var eventToSend = LinkLifecycleEventProto.LinkLifecycleEvent.newBuilder()
                .setLinkUpdated(updatedPayload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, String.valueOf(link.id()), eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        try {
            var result = kafkaTemplate.send(producerRecord).get();
            log.info("Successfully published LinkUpdated event for link ID: {} to partition: {} offset: {}",
                    link.id(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to publish LinkUpdated event for link ID: {}, shortURL: {}, reason: {}",
                    link.id(),
                    link.shortUrl(),
                    e.getMessage(),
                    e);
        }
    }
}
