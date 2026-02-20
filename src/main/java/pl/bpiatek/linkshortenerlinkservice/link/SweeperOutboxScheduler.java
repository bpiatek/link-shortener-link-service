package pl.bpiatek.linkshortenerlinkservice.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import pl.bpiatek.linkshortenerlinkservice.link.OutboxRepository.OutboxRecord;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


class SweeperOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(SweeperOutboxScheduler.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final LinkLifecycleKafkaProducer kafkaProducer;

    private final ReentrantLock lock = new ReentrantLock();

    SweeperOutboxScheduler(OutboxRepository outboxRepository, ObjectMapper objectMapper, LinkLifecycleKafkaProducer kafkaProducer) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.kafkaProducer = kafkaProducer;
    }


    @Scheduled(fixedDelayString = "${app.scheduling.sweeper-delay:5000}")
    public void recoverStalledEvents() {
        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    executeRecovery();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Sweeper skip: recovery already in progress or database is slow.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sweeper thread interrupted during lock acquisition");
        }
    }

    private void executeRecovery() {
        var events = outboxRepository.fetchStalledEvents(50);
        if (events.isEmpty()) {
            return;
        }

        log.warn("Sweeper found {} stalled events. Initiating recovery...", events.size());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var event : events) {
                executor.submit(() -> processSingleEvent(event));
            }
        } catch (Exception e) {
            log.error("Critical failure in Sweeper executor", e);
        }
    }

    private void processSingleEvent(OutboxRecord event) {
        try {
            var link = objectMapper.readValue(event.payload(), Link.class);

            kafkaProducer.sendLifecycleEvent(link, event.eventType());

            outboxRepository.markAsProcessedById(event.id());
            log.info("Sweeper successfully recovered event ID: {} of type: {}", event.id(), event.eventType());
        } catch (Exception e) {
            log.error("Sweeper failed to recover event ID: {}. Reason: {}",
                    event.id(), e.getMessage());
        }
    }
}