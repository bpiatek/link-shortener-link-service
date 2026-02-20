package pl.bpiatek.linkshortenerlinkservice.link;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkLifecycleKafkaProducerCreateEventTest {

    private static final String TEST_TOPIC = "test-topic";

    @Mock
    private KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, LinkLifecycleEvent>> producerRecordCaptor;

    private LinkLifecycleKafkaProducer linkLifecycleKafkaProducer;
    private CompletableFuture<SendResult<String, LinkLifecycleEvent>> future;
    private Link link;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(
                Instant.parse("2025-08-22T10:00:00Z"),
                ZoneOffset.UTC);
        linkLifecycleKafkaProducer = new LinkLifecycleKafkaProducer(TEST_TOPIC, kafkaTemplate, clock);
        link = LinkStubs.aLink();
    }

    private void mockSuccessfulSend() {
        SendResult<String, LinkLifecycleEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        given(sendResult.getRecordMetadata()).willReturn(metadata);

        given(kafkaTemplate.send((ProducerRecord<String, LinkLifecycleEvent>) any())).willReturn(CompletableFuture.completedFuture(sendResult));
    }

    @Test
    void shouldSendMessageAndIncrementSuccessMetric() {
        //given
        mockSuccessfulSend();

        // when
        linkLifecycleKafkaProducer.sendLifecycleEvent(link, LinkEventType.LINK_CREATED);

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
        var source = record.headers().lastHeader("source");
        softly.assertThat(source).isNotNull();
        softly.assertThat(new String(source.value(), UTF_8)).isEqualTo("link-service");
    }
}