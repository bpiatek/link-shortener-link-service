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

import java.util.UUID;
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

    @Mock
    private LinkLifecycleEventFactory eventFactory;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, LinkLifecycleEvent>> producerRecordCaptor;

    private LinkLifecycleKafkaProducer linkLifecycleKafkaProducer;
    private CompletableFuture<SendResult<String, LinkLifecycleEvent>> future;
    private Link link;

    @BeforeEach
    void setUp() {
        linkLifecycleKafkaProducer = new LinkLifecycleKafkaProducer(TEST_TOPIC, kafkaTemplate, eventFactory);
        link = LinkStubs.aLink();
    }

    @Test
    void shouldSendMessageAndIncrementSuccessMetric() {
        // given
        var eventId = UUID.randomUUID();

        var expectedProtobufEvent = LinkLifecycleEvent.newBuilder().build();

        given(eventFactory.createEvent(eventId, link, LinkEventType.LINK_CREATED))
                .willReturn(expectedProtobufEvent);

        mockSuccessfulSend();

        // when
        linkLifecycleKafkaProducer.sendLifecycleEvent(eventId, link, LinkEventType.LINK_CREATED);

        // then
        verify(kafkaTemplate).send(producerRecordCaptor.capture());
        var sentRecord = producerRecordCaptor.getValue();

        var softly = new SoftAssertions();
        assertRecordBasics(sentRecord, expectedProtobufEvent, softly);
        assertHeaders(sentRecord, softly);
        softly.assertAll();
    }

    private void mockSuccessfulSend() {
        SendResult<String, LinkLifecycleEvent> sendResult = mock(SendResult.class);
        var metadata = mock(RecordMetadata.class);
        given(sendResult.getRecordMetadata()).willReturn(metadata);

        given(kafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(sendResult));
    }

    private void assertRecordBasics(ProducerRecord<String, LinkLifecycleEvent> record,
                                    LinkLifecycleEvent expectedEvent,
                                    SoftAssertions softly) {
        softly.assertThat(record.topic()).isEqualTo(TEST_TOPIC);
        softly.assertThat(record.key()).isEqualTo(String.valueOf(link.id()));

        softly.assertThat(record.value()).isEqualTo(expectedEvent);
    }

    private void assertHeaders(ProducerRecord<String, LinkLifecycleEvent> record, SoftAssertions softly) {
        var source = record.headers().lastHeader("source");
        softly.assertThat(source).isNotNull();
        softly.assertThat(new String(source.value(), UTF_8)).isEqualTo("link-service");
    }
}