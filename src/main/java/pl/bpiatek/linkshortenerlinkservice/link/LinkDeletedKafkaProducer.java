package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkDeleted;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.time.Clock;

import static java.nio.charset.StandardCharsets.UTF_8;

class LinkDeletedKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LinkDeletedKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;
    private final Clock clock;

    public LinkDeletedKafkaProducer(String topicName,
                                    KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.clock = clock;
    }

    void sendLinkDeletedEvent(Link link) {
        var deletedPayload = LinkDeleted.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setDeletedAt(Timestamp.newBuilder()
                        .setSeconds(clock.instant().getEpochSecond())
                        .setNanos(clock.instant().getNano()).build())
                .build();

        var eventToSend = LinkLifecycleEvent.newBuilder()
                .setLinkDeleted(deletedPayload)
                .build();

        var producerRecord = new ProducerRecord<>(topicName, String.valueOf(link.id()), eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(UTF_8)));

        try {
            var result = kafkaTemplate.send(producerRecord).get();
            log.info("Successfully published LinkDeleted event for link ID: {} to partition: {} offset: {}",
                    link.id(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to publish LinkDeleted event for link ID: {}, shortURL: {}, reason: {}",
                    link.id(),
                    link.shortUrl(),
                    e.getMessage(),
                    e);
        }
    }
}
