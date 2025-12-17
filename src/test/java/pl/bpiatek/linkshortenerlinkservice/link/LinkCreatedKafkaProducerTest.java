package pl.bpiatek.linkshortenerlinkservice.link;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkCreatedKafkaProducerTest {

    private static final String TEST_TOPIC = "test-topic";

    @Mock
    private KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, LinkLifecycleEvent>> producerRecordCaptor;

    private LinkCreatedKafkaProducer linkCreatedKafkaProducer;
    private CompletableFuture<SendResult<String, LinkLifecycleEvent>> future;
    private Link link;

    @BeforeEach
    void setUp() {
        linkCreatedKafkaProducer = new LinkCreatedKafkaProducer(TEST_TOPIC, kafkaTemplate);
        link = LinkStubs.aLink();
        future = new CompletableFuture<>();

        given(kafkaTemplate.send(any(ProducerRecord.class))).willReturn(future);
    }

    @Test
    void shouldSendMessageAndIncrementSuccessMetric() {
        // when
        linkCreatedKafkaProducer.sendLinkCreatedEvent(link);
        future.complete(mock(SendResult.class));

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();
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