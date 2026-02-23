package pl.bpiatek.linkshortenerlinkservice.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.UnableToGenerateUniqueShortUrlException;


class RandomShortUrlCreationStrategy implements LinkCreationStrategy {

    private static final Logger log = LoggerFactory.getLogger(RandomShortUrlCreationStrategy.class);
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final LinkMapper linkMapper;
    private final ShortUrlGenerator shortUrlGenerator;
    private final LinkTransactionalPersister linkTransactionalPersister;

    RandomShortUrlCreationStrategy(LinkMapper linkMapper, ShortUrlGenerator shortUrlGenerator, LinkTransactionalPersister linkTransactionalPersister) {
        this.linkMapper = linkMapper;
        this.shortUrlGenerator = shortUrlGenerator;
        this.linkTransactionalPersister = linkTransactionalPersister;
    }

    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String ignoredShortUrl, boolean isActive, String title, ApplicationEventPublisher eventPublisher) {
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            var generatedShortUrl = shortUrlGenerator.generate();
            var linkToSave = linkMapper.toLink(userId, longUrl, generatedShortUrl, isActive, false, title);

            try {
                return linkTransactionalPersister.persistAndPublish(linkToSave, eventPublisher);
            } catch (DataIntegrityViolationException e) {
                log.warn("Collision while creating short url: {}", generatedShortUrl);
            }
        }
        throw new UnableToGenerateUniqueShortUrlException(MAX_GENERATION_ATTEMPTS);
    }

    @Override
    public CreationStrategyType getType() {
        return CreationStrategyType.RANDOM;
    }
}
