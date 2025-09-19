package pl.bpiatek.linkshortenerlinkservice.link;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Profile("test")
public class TestLinkEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TestLinkEventConsumer.class);

    private CountDownLatch latch = new CountDownLatch(1);
    private ConsumerRecord<String, LinkLifecycleEventProto.LinkLifecycleEvent> payload;

    @KafkaListener(
            topics = "${topic.link.lifecycle}",
            groupId = "test-consumer-group",
            autoStartup = "true"
    )
    public void receive(ConsumerRecord<String, LinkLifecycleEventProto.LinkLifecycleEvent> consumerRecord) {
        log.info("Test consumer received message with key: {}", consumerRecord.key());
        payload = consumerRecord;
        latch.countDown();
    }

    public ConsumerRecord<String, LinkLifecycleEventProto.LinkLifecycleEvent> awaitRecord(long timeout, TimeUnit unit) throws InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new IllegalStateException("No message received in the allotted time");
        }
        return payload;
    }

    public void reset() {
        latch = new CountDownLatch(1);
        payload = null;
    }
}