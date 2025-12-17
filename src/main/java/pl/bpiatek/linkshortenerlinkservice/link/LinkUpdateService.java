package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.exception.LinkNotFoundException;

import java.time.Clock;

class LinkUpdateService {

    private final  LinkRepository linkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final LinkMapper linkMapper;

    LinkUpdateService(
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

        var updatedLink = new Link(
                existingLink.id(),
                existingLink.userId(),
                existingLink.shortUrl(),
                request.longUrl() != null ? request.longUrl() : existingLink.longUrl(),
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
}
