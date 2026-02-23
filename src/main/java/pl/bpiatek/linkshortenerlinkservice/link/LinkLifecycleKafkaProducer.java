package pl.bpiatek.linkshortenerlinkservice.link;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class LinkLifecycleKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LinkLifecycleKafkaProducer.class);

    private static final String SOURCE_HEADER_VALUE = "link-service";

    private final String topicName;
    private final KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;
    private final LinkLifecycleEventFactory eventFactory;

    public LinkLifecycleKafkaProducer(String topicName,
                                      KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate,
                                      LinkLifecycleEventFactory eventFactory) {
        this.topicName = topicName;
        this.kafkaTemplate = kafkaTemplate;
        this.eventFactory = eventFactory;
    }

    void sendLifecycleEvent(UUID eventId, Link link, LinkEventType eventType) {
        var protobufEvent = eventFactory.createEvent(eventId, link, eventType);
        var linkIdStr = String.valueOf(link.id());

        sendPreBuiltEvent(linkIdStr, protobufEvent, eventType);
    }

    void sendPreBuiltEvent(String aggregateIdStr, LinkLifecycleEvent eventToSend, LinkEventType eventType) {
        var producerRecord = new ProducerRecord<>(topicName, aggregateIdStr, eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(StandardCharsets.UTF_8)));

        try {
            // Wait for Kafka ACK before allowing the caller to mark the Outbox DB row as processed
            var result = kafkaTemplate.send(producerRecord).join();

            log.info("Successfully published {} event for link ID: {} to partition: {} offset: {}",
                    eventType,
                    aggregateIdStr,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (CompletionException e) {
            var realCause = e.getCause() != null ? e.getCause() : e;

            log.error("Failed to publish {} event for link ID: {}. Reason: {}",
                    eventType, aggregateIdStr, realCause.getMessage(), realCause);

            // We MUST throw the exception so the caller (Fast-Track or Sweeper) knows it failed
            throw new RuntimeException("Kafka send failed for " + eventType, realCause);
        }
    }

    CompletableFuture<SendResult<String, LinkLifecycleEvent>> sendAsyncPreBuiltEvent(
            String aggregateIdStr,
            LinkLifecycleEvent eventToSend) {

        var producerRecord = new ProducerRecord<>(topicName, aggregateIdStr, eventToSend);
        producerRecord.headers().add(new RecordHeader("source", SOURCE_HEADER_VALUE.getBytes(StandardCharsets.UTF_8)));

        return kafkaTemplate.send(producerRecord);
    }
}