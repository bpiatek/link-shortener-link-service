package pl.bpiatek.linkshortenerlinkservice.link;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.api.dto.UpdateLinkRequest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LinkFacade {

    private static final Logger log = LoggerFactory.getLogger(LinkFacade.class);

    private final Map<CreationStrategyType, LinkCreationStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;
    private final LinkUpdateService linkUpdateService;

    LinkFacade(
            List<LinkCreationStrategy> strategiesLIst,
            ApplicationEventPublisher eventPublisher,
            LinkUpdateService linkUpdateService) {
        this.strategies = strategiesLIst.stream()
                .collect(Collectors.toUnmodifiableMap(LinkCreationStrategy::getType, Function.identity()));
        this.eventPublisher = eventPublisher;
        this.linkUpdateService = linkUpdateService;
    }

    @Transactional
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl, Boolean isActive, String title) {
        var strategyType = getStrategyType(shortUrl);
        log.info("Selected link creation strategy: {}", strategyType);
        var chosenStrategy = strategies.get(strategyType);
        if (chosenStrategy == null) {
            throw new IllegalStateException("No LinkCreationStrategy bean found for type: " + strategyType);
        }
        return chosenStrategy.createLink(userId, longUrl, shortUrl, isActive, title, eventPublisher);
    }

    public LinkDto updateLink(String userId, Long linkId, UpdateLinkRequest request) {
        return linkUpdateService.update(userId, linkId, request);
    }

    private CreationStrategyType getStrategyType(String shortUrl) {
        if (shortUrl != null && !shortUrl.isBlank()) {
            return CreationStrategyType.CUSTOM;
        } else {
            return CreationStrategyType.RANDOM;
        }
    }
}
