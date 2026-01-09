package pl.bpiatek.linkshortenerlinkservice;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@ActiveProfiles("test")
public class TestKafkaConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(TestKafkaConsumer.class);

    private CountDownLatch latch = new CountDownLatch(1);
    private ConsumerRecord<String, T> payload;

    public void handle(ConsumerRecord<String, T> record) {
        log.info("Test consumer received record with key: '{}' and payload '{}'", record.key(), record.value());
        payload = record;
        latch.countDown();
    }

    public ConsumerRecord<String, T> awaitRecord(long timeout, TimeUnit unit) throws InterruptedException {
        if (!latch.await(timeout, unit)) {
            throw new IllegalStateException("No event received in the allotted time");
        }
        return payload;
    }

    public void reset() {
        latch = new CountDownLatch(1);
        payload = null;
    }
}
