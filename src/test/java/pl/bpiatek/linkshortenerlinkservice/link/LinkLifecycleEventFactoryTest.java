package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LinkLifecycleEventFactoryTest {

    private static final Instant FIXED_NOW = Instant.parse("2025-10-01T12:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private Clock clock;

    private LinkLifecycleEventFactory factory;
    private Link dummyLink;

    @BeforeEach
    void setUp() {
        factory = new LinkLifecycleEventFactory(clock);

        dummyLink = new Link(
                100L,
                "user-123",
                "shorty",
                "https://example.com/long-url",
                "My Awesome Link",
                "Some notes",
                true,
                false,
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-02-01T10:00:00Z"),
                null
        );
    }

    @Test
    void shouldMapToLinkCreatedEvent() {
        // when
        var protobufEvent = factory.createEvent(EVENT_ID, dummyLink, LinkEventType.LINK_CREATED);

        // then
        assertSoftly(softly -> {
            softly.assertThat(protobufEvent.getEventId()).isEqualTo(EVENT_ID.toString());
            softly.assertThat(protobufEvent.hasLinkCreated()).isTrue();

            var createdPayload = protobufEvent.getLinkCreated();
            softly.assertThat(createdPayload.getLinkId()).isEqualTo("100");
            softly.assertThat(createdPayload.getUserId()).isEqualTo("user-123");
            softly.assertThat(createdPayload.getShortUrl()).isEqualTo("shorty");
            softly.assertThat(createdPayload.getLongUrl()).isEqualTo("https://example.com/long-url");
            softly.assertThat(createdPayload.getTitle()).isEqualTo("My Awesome Link");
            softly.assertThat(createdPayload.getIsActive()).isTrue();

            var expectedTimestamp = toProtobufTimestamp(dummyLink.createdAt());
            softly.assertThat(createdPayload.getCreatedAt()).isEqualTo(expectedTimestamp);
        });
    }

    @Test
    void shouldMapToLinkUpdatedEvent() {
        // when
        var protobufEvent = factory.createEvent(EVENT_ID, dummyLink, LinkEventType.LINK_UPDATED);

        // then
        assertSoftly(softly -> {
            softly.assertThat(protobufEvent.getEventId()).isEqualTo(EVENT_ID.toString());
            softly.assertThat(protobufEvent.hasLinkUpdated()).isTrue();

            var updatedPayload = protobufEvent.getLinkUpdated();
            softly.assertThat(updatedPayload.getLinkId()).isEqualTo("100");
            softly.assertThat(updatedPayload.getUserId()).isEqualTo("user-123");
            softly.assertThat(updatedPayload.getShortUrl()).isEqualTo("shorty");
            softly.assertThat(updatedPayload.getLongUrl()).isEqualTo("https://example.com/long-url");
            softly.assertThat(updatedPayload.getTitle()).isEqualTo("My Awesome Link");
            softly.assertThat(updatedPayload.getIsActive()).isTrue();

            var expectedTimestamp = toProtobufTimestamp(dummyLink.updatedAt());
            softly.assertThat(updatedPayload.getUpdatedAt()).isEqualTo(expectedTimestamp);
        });
    }

    @Test
    void shouldMapToLinkDeletedEvent() {
        // given
        given(clock.instant()).willReturn(FIXED_NOW);

        // when
        var protobufEvent = factory.createEvent(EVENT_ID, dummyLink, LinkEventType.LINK_DELETED);

        // then
        assertSoftly(softly -> {
            softly.assertThat(protobufEvent.getEventId()).isEqualTo(EVENT_ID.toString());
            softly.assertThat(protobufEvent.hasLinkDeleted()).isTrue();

            var deletedPayload = protobufEvent.getLinkDeleted();
            softly.assertThat(deletedPayload.getLinkId()).isEqualTo("100");
            softly.assertThat(deletedPayload.getUserId()).isEqualTo("user-123");
            softly.assertThat(deletedPayload.getShortUrl()).isEqualTo("shorty");

            var expectedTimestamp = toProtobufTimestamp(FIXED_NOW);
            softly.assertThat(deletedPayload.getDeletedAt()).isEqualTo(expectedTimestamp);
        });
    }

    @Test
    void shouldThrowExceptionForUnknownEventType() {
        // given
        assertThatThrownBy(() -> factory.createEvent(EVENT_ID, dummyLink, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Timestamp toProtobufTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}