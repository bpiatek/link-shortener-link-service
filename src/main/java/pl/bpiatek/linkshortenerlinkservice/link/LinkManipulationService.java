package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.exception.LinkNotFoundException;

import java.time.Clock;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static pl.bpiatek.linkshortenerlinkservice.link.UrlSanitizer.prependProtocolIfMissing;

class LinkManipulationService {

    private final  LinkRepository linkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final LinkMapper linkMapper;
    private final  Integer olderThanInDays;
    private final OutboxRepository outboxRepository;
    private final LinkLifecycleEventFactory eventFactory;

    LinkManipulationService(
            LinkRepository linkRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            LinkMapper linkMapper,
            Integer days, OutboxRepository outboxRepository, LinkLifecycleEventFactory eventFactory) {
        this.linkRepository = linkRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.linkMapper = linkMapper;
        this.olderThanInDays = days;
        this.outboxRepository = outboxRepository;
        this.eventFactory = eventFactory;
    }

    @Transactional
    LinkDto update(String userId, Long linkId, UpdateLinkRequest request) {
        var existingLink = linkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new LinkNotFoundException("Link not found or access denied"));

        var cleanUrl = request.longUrl() != null
                ? prependProtocolIfMissing(request.longUrl())
                : existingLink.longUrl();

        var updatedLink = new Link(
                existingLink.id(),
                existingLink.userId(),
                existingLink.shortUrl(),
                cleanUrl,
                request.title() != null ? request.title() : existingLink.title(),
                existingLink.notes(),
                request.isActive() != null ? request.isActive() : existingLink.isActive(),
                existingLink.isCustom(),
                existingLink.createdAt(),
                clock.instant(),
                existingLink.expiresAt()
        );

        linkRepository.update(updatedLink);

        var eventId = UUID.randomUUID();
        var protobufEvent = eventFactory.createEvent(eventId, updatedLink, LinkEventType.LINK_UPDATED);

        outboxRepository.saveEvent(String.valueOf(updatedLink.id()), LinkEventType.LINK_UPDATED, protobufEvent);

        eventPublisher.publishEvent(new LinkUpdatedApplicationEvent(eventId, updatedLink));

        return linkMapper.toLinkDto(updatedLink);
    }

    @Transactional
    public void deleteLink(String userId, Long linkId) {
        var link = linkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new LinkNotFoundException("Link not found or access denied"));

        executeDeleteAndPublish(link);
    }

    @Transactional
    public int releaseDeactivatedVanityLinks() {
        var cutoffDate = clock.instant().minus(olderThanInDays, DAYS);

        var deletedLinks = linkRepository.deleteAndReturnDeactivatedCustomLinksOlderThan(cutoffDate);
        if (deletedLinks.isEmpty()) {
            return 0;
        }

        var batchArgs = deletedLinks.stream().map(link -> {
            var eventId = UUID.randomUUID();
            var protobufEvent = eventFactory.createEvent(eventId, link, LinkEventType.LINK_DELETED);

            return new MapSqlParameterSource()
                    .addValue("id", eventId)
                    .addValue("aggregate_id", String.valueOf(link.id()))
                    .addValue("topic", "link-lifecycle-events")
                    .addValue("event_type", LinkEventType.LINK_DELETED.name())
                    .addValue("payload", protobufEvent.toByteArray());
        }).toArray(SqlParameterSource[]::new);

        outboxRepository.saveEventsInBatch(batchArgs);

        return deletedLinks.size();
    }

    private void executeDeleteAndPublish(Link link) {
        linkRepository.deleteByIdAndUserId(link.id(), link.userId());

        var eventId = UUID.randomUUID();
        var protobufEvent = eventFactory.createEvent(eventId, link, LinkEventType.LINK_DELETED);

        outboxRepository.saveEvent(String.valueOf(link.id()), LinkEventType.LINK_DELETED, protobufEvent);

        eventPublisher.publishEvent(new LinkDeletedApplicationEvent(eventId, link));
    }
}
