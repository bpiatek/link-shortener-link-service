package pl.bpiatek.linkshortenerlinkservice.link;

import com.google.protobuf.Timestamp;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.time.Clock;
import java.util.UUID;

class LinkLifecycleEventFactory {

    private final Clock clock;

    LinkLifecycleEventFactory(Clock clock) {
        this.clock = clock;
    }


    LinkLifecycleEventProto.LinkLifecycleEvent createEvent(UUID eventId, Link link, LinkEventType eventType) {
        var builder = LinkLifecycleEventProto.LinkLifecycleEvent.newBuilder()
                .setEventId(eventId.toString());

        switch (eventType) {
            case LINK_CREATED -> builder.setLinkCreated(mapToCreated(link));
            case LINK_UPDATED -> builder.setLinkUpdated(mapToUpdated(link));
            case LINK_DELETED -> builder.setLinkDeleted(mapToDeleted(link));
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        return builder.build();
    }

    private LinkLifecycleEventProto.LinkCreated mapToCreated(Link link) {
        return LinkLifecycleEventProto.LinkCreated.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setLongUrl(link.longUrl())
                .setIsActive(link.isActive())
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(link.createdAt().getEpochSecond())
                        .setNanos(link.createdAt().getNano()).build())
                .setTitle(link.title() == null ? "" : link.title())
                .build();
    }

    private LinkLifecycleEventProto.LinkUpdated mapToUpdated(Link link) {
        return LinkLifecycleEventProto.LinkUpdated.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setLongUrl(link.longUrl())
                .setIsActive(link.isActive())
                .setUpdatedAt(Timestamp.newBuilder()
                        .setSeconds(link.updatedAt().getEpochSecond())
                        .setNanos(link.updatedAt().getNano()).build())
                .setTitle(link.title() == null ? "" : link.title())
                .build();
    }

    private LinkLifecycleEventProto.LinkDeleted mapToDeleted(Link link) {
        var now = clock.instant();

        return LinkLifecycleEventProto.LinkDeleted.newBuilder()
                .setLinkId(String.valueOf(link.id()))
                .setUserId(link.userId())
                .setShortUrl(link.shortUrl())
                .setDeletedAt(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano()).build())
                .build();
    }
}
