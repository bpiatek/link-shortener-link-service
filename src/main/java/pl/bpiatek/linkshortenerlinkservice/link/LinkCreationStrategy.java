package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

interface LinkCreationStrategy {
    CreateLinkResponse createLink(
            String userId,
            String longUrl,
            String shortUrl,
            ApplicationEventPublisher applicationEventPublisher);

    CreationStrategyType getType();
}
