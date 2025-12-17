package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.exception.LinkNotFoundException;

import java.time.Clock;

import static pl.bpiatek.linkshortenerlinkservice.link.UrlSanitizer.prependProtocolIfMissing;

class LinkManipulationService {

    private final  LinkRepository linkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final LinkMapper linkMapper;

    LinkManipulationService(
            LinkRepository linkRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            LinkMapper linkMapper) {
        this.linkRepository = linkRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.linkMapper = linkMapper;
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
                existingLink.createdAt(),
                clock.instant(),
                existingLink.expiresAt()
        );

        linkRepository.update(updatedLink);

        eventPublisher.publishEvent(new LinkUpdatedApplicationEvent(updatedLink));

        return linkMapper.toLinkDto(updatedLink);
    }

    @Transactional
    public void deleteLink(String userId, Long linkId) {
        var link = linkRepository.findByIdAndUserId(linkId, userId)
                .orElseThrow(() -> new LinkNotFoundException("Link not found or access denied"));

        linkRepository.deleteByIdAndUserId(linkId, userId);

        eventPublisher.publishEvent(new LinkDeletedApplicationEvent(link));
    }
}
