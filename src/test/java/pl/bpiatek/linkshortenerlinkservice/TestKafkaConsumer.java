package pl.bpiatek.linkshortenerlinkservice;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@ActiveProfiles("test")
public class TestKafkaConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(TestKafkaConsumer.class);

    private final BlockingQueue<ConsumerRecord<String, T>> records = new LinkedBlockingQueue<>();

    public void handle(ConsumerRecord<String, T> record) {
        log.info("Test consumer received record: {}", record.key());
        records.add(record);
    }

    public ConsumerRecord<String, T> awaitRecord(long timeout, TimeUnit unit) throws InterruptedException {
        var record = records.poll(timeout, unit);
        if (record == null) {
            throw new IllegalStateException("No event received in the allotted time (" + timeout + " " + unit + ")");
        }
        return record;
    }

    public void reset() {
        records.clear();
    }
}
