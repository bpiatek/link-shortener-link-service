package pl.bpiatek.linkshortenerlinkservice.link;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String METRIC_NAME = "link_service_kafka_messages_total";

    @Mock
    private KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, LinkLifecycleEvent>> producerRecordCaptor;

    @Captor
    private ArgumentCaptor<String[]> tagsCaptor;

    private KafkaProducerService kafkaProducerService;
    private Counter counter;
    private CompletableFuture<SendResult<String, LinkLifecycleEvent>> future;
    private Link link;

    @BeforeEach
    void setUp() {
        kafkaProducerService = new KafkaProducerService(TEST_TOPIC, kafkaTemplate, meterRegistry);
        link = LinkStubs.aLink();
        future = new CompletableFuture<>();
        counter = mock(Counter.class);

        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(future);
        given(meterRegistry.counter(anyString(), any(String[].class))).willReturn(counter);
    }

    @Test
    void shouldSendMessageAndIncrementSuccessMetric() {
        // when
        kafkaProducerService.sendLinkCreatedEvent(link);
        future.complete(mock(SendResult.class));

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();

        verify(meterRegistry).counter(eq(METRIC_NAME), tagsCaptor.capture());
        verify(counter).increment();

        assertThat(tagsCaptor.getValue()).containsExactly("topic", TEST_TOPIC, "status", "success");
    }

    @Test
    void shouldIncrementFailureMetricOnException() {
        // when
        kafkaProducerService.sendLinkCreatedEvent(link);
        future.completeExceptionally(new RuntimeException("Kafka down"));

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();

        verify(meterRegistry).counter(eq(METRIC_NAME), tagsCaptor.capture());
        verify(counter).increment();

        assertThat(tagsCaptor.getValue())
                .contains("topic", TEST_TOPIC)
                .contains("status", "failure")
                .contains("exception", "RuntimeException");
    }

    private void assertRecordBasics(ProducerRecord<String, LinkLifecycleEvent> record, SoftAssertions softly) {
        softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);
        softly.assertThat(record.key()).isEqualTo("1");
        softly.assertThat(record.value().getLinkCreated().getLinkId()).isEqualTo("1");
    }

    private void assertHeaders(ProducerRecord<String, LinkLifecycleEvent> record, SoftAssertions softly) {
        var traceId = record.headers().lastHeader("trace-id");
        softly.assertThat(traceId).isNotNull();
        softly.assertThat(new String(traceId.value(), UTF_8)).isNotEmpty();

        var source = record.headers().lastHeader("source");
        softly.assertThat(source).isNotNull();
        softly.assertThat(new String(source.value(), UTF_8)).isEqualTo("link-service");
    }
}