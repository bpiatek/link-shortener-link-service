package pl.bpiatek.linkshortenerlinkservice.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Semaphore;

class FastTrackOutboxListener {

    private static final Logger log = LoggerFactory.getLogger(FastTrackOutboxListener.class);
    private final Semaphore semaphore;

    private final LinkLifecycleKafkaProducer kafkaProducer;
    private final OutboxRepository outboxRepository;

    public FastTrackOutboxListener(LinkLifecycleKafkaProducer kafkaProducer,
                                   OutboxRepository outboxRepository,
                                   int maxConcurrency) {
        this.kafkaProducer = kafkaProducer;
        this.outboxRepository = outboxRepository;
        this.semaphore = new Semaphore(maxConcurrency);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLinkCreatedEvent(LinkCreatedApplicationEvent event) {
        processFastTrack(event.link(), LinkEventType.LINK_CREATED);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLinkUpdatedEvent(LinkUpdatedApplicationEvent event) {
        processFastTrack(event.link(), LinkEventType.LINK_UPDATED);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLinkDeletedEvent(LinkDeletedApplicationEvent event) {
        processFastTrack(event.link(), LinkEventType.LINK_DELETED);
    }

    private void processFastTrack(Link link, LinkEventType eventType) {
        if (!semaphore.tryAcquire()) {
            log.warn("Fast-Track throttled for ID: {}. Sweeper will handle it.", link.id());
            return;
        }

        var id = String.valueOf(link.id());

        try {
            // Send to the consolidated producer (blocks Virtual Thread until ACK)
            log.info("Attempting to publish {} to Kafka for ID: {}", eventType, id);
            kafkaProducer.sendLifecycleEvent(link, eventType);

            log.info("Kafka ACK received. Marking Outbox as processed for ID: {}", id);
            outboxRepository.markAsProcessed(id, eventType);

            log.info("Fast-Track successful for Link ID: {} with event: {}", id, eventType);
        } catch (Exception e) {
            log.warn("Fast-Track failed for Link ID: {} with event: {}. Sweeper will recover this. Reason: {}",
                    id, eventType, e.getMessage());
        }
    }
}