package pl.bpiatek.linkshortenerlinkservice.link;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.exception.LinkNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pl.bpiatek.linkshortenerlinkservice.link.UrlSanitizer.prependProtocolIfMissing;

public class LinkFacade {

    private static final Logger log = LoggerFactory.getLogger(LinkFacade.class);

    private final Map<CreationStrategyType, LinkCreationStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;
    private final LinkUpdateService linkUpdateService;
    private final LinkRetriever linkRetriever;

    LinkFacade(
            List<LinkCreationStrategy> strategiesLIst,
            ApplicationEventPublisher eventPublisher,
            LinkUpdateService linkUpdateService,
            LinkRetriever linkRetriever) {
        this.strategies = strategiesLIst.stream()
                .collect(Collectors.toUnmodifiableMap(LinkCreationStrategy::getType, Function.identity()));
        this.eventPublisher = eventPublisher;
        this.linkUpdateService = linkUpdateService;
        this.linkRetriever = linkRetriever;
    }

    @Transactional
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl, Boolean isActive, String title) {
        var cleanUrl = prependProtocolIfMissing(longUrl);

        var strategyType = getStrategyType(shortUrl);
        log.info("Selected link creation strategy: {}", strategyType);
        var chosenStrategy = strategies.get(strategyType);
        if (chosenStrategy == null) {
            throw new IllegalStateException("No LinkCreationStrategy bean found for type: " + strategyType);
        }
        return chosenStrategy.createLink(userId, cleanUrl, shortUrl, isActive, title, eventPublisher);
    }

    public LinkDto updateLink(String userId, Long linkId, UpdateLinkRequest request) {
        log.info("Updating link with ID: {}", linkId);
        return linkUpdateService.update(userId, linkId, request);
    }

    public LinkDto getLink(String userId, Long linkId) {
        return linkRetriever.getLink(userId, linkId);
    }


    private CreationStrategyType getStrategyType(String shortUrl) {
        if (shortUrl != null && !shortUrl.isBlank()) {
            return CreationStrategyType.CUSTOM;
        } else {
            return CreationStrategyType.RANDOM;
        }
    }
}
