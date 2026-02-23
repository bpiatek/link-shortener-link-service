package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;


class SweeperOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(SweeperOutboxScheduler.class);

    private final OutboxRepository outboxRepository;
    private final LinkLifecycleKafkaProducer kafkaProducer;
    private final TransactionTemplate transactionTemplate;
    private final ReentrantLock lock = new ReentrantLock();

    SweeperOutboxScheduler(OutboxRepository outboxRepository,
                           LinkLifecycleKafkaProducer kafkaProducer,
                           TransactionTemplate transactionTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaProducer = kafkaProducer;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.sweeper-delay:5000}")
    public void recoverStalledEvents() {
        try {
            if (lock.tryLock(1, SECONDS)) {
                try {
                    executeRecovery();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Sweeper skip: recovery already in progress.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sweeper thread interrupted during lock acquisition");
        }
    }

    private void executeRecovery() {
        transactionTemplate.executeWithoutResult(status -> {
            var events = outboxRepository.fetchStalledEvents(50);
            if (events.isEmpty()) {
                return;
            }

            log.info("Sweeper found {} stalled events. Initiating recovery...", events.size());
            var futures = dispatchToKafkaAsync(events);
            var successfulEventIds = awaitAndCollectSuccessfulIds(futures);

            if (!successfulEventIds.isEmpty()) {
                outboxRepository.markAsProcessedInBatch(successfulEventIds);
                log.info("Sweeper successfully recovered {} events.", successfulEventIds.size());
            }
        });
    }

    private List<CompletableFuture<UUID>> dispatchToKafkaAsync(List<OutboxRepository.OutboxRecord> events) {
        return events.stream()
                .map(this::sendSingleEventAsync)
                .toList();
    }

    private List<UUID> awaitAndCollectSuccessfulIds(List<CompletableFuture<UUID>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    private CompletableFuture<UUID> sendSingleEventAsync(OutboxRepository.OutboxRecord event) {
        try {
            var protobufEvent = LinkLifecycleEventProto.LinkLifecycleEvent.parseFrom(event.payload());

            return kafkaProducer.sendAsyncPreBuiltEvent(event.aggregateId(), protobufEvent)
                    .thenApply(sendResult -> event.id()) // Return UUID on success
                    .exceptionally(ex -> {
                        log.error("Kafka rejected event ID: {}", event.id(), ex);
                        return null;
                    });

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse Protobuf for event ID: {}", event.id(), e);
            return CompletableFuture.completedFuture(null);
        }
    }
}