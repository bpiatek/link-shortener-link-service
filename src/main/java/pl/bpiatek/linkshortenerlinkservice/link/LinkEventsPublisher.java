package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class LinkEventsPublisher {

    private final LinkCreatedKafkaProducer linkCreatedKafkaProducer;
    private final LinkUpdatedKafkaProducer linkUpdatedKafkaProducer;
    private final LinkDeletedKafkaProducer linkDeletedKafkaProducer;

    LinkEventsPublisher(LinkCreatedKafkaProducer linkCreatedKafkaProducer, LinkUpdatedKafkaProducer linkUpdatedKafkaProducer, LinkDeletedKafkaProducer linkDeletedKafkaProducer) {
        this.linkCreatedKafkaProducer = linkCreatedKafkaProducer;
        this.linkUpdatedKafkaProducer = linkUpdatedKafkaProducer;
        this.linkDeletedKafkaProducer = linkDeletedKafkaProducer;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleLinkCreatedEvent(LinkCreatedApplicationEvent event) {
        linkCreatedKafkaProducer.sendLinkCreatedEvent(event.link());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleLinkUpdatedEvent(LinkUpdatedApplicationEvent event) {
        linkUpdatedKafkaProducer.sendLinkUpdatedEvent(event.link());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleLinkDeletedEvent(LinkDeletedApplicationEvent event) {
        linkDeletedKafkaProducer.sendLinkDeletedEvent(event.link());
    }
}
