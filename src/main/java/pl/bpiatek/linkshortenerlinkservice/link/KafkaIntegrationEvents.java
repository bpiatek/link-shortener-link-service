package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class KafkaIntegrationEvents {

    private final KafkaProducerService kafkaProducerService;

    KafkaIntegrationEvents(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleLinkCreatedEvent(LinkCreatedApplicationEvent event) {
        kafkaProducerService.sendLinkCreatedEvent(event.link());
    }
}
