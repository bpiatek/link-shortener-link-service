package pl.bpiatek.linkshortenerlinkservice.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.UnableToGenerateUniqueShortUrlException;

import java.util.Objects;


class RandomShortUrlCreationStrategy implements LinkCreationStrategy {

    private static final Logger log = LoggerFactory.getLogger(RandomShortUrlCreationStrategy.class);
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final ShortUrlGenerator shortUrlGenerator;
    private final TransactionTemplate transactionTemplate;

    RandomShortUrlCreationStrategy(LinkRepository linkRepository, LinkMapper linkMapper, ShortUrlGenerator shortUrlGenerator, TransactionTemplate transactionTemplate) {
        this.linkRepository = linkRepository;
        this.linkMapper = linkMapper;
        this.shortUrlGenerator = shortUrlGenerator;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String ignoredShortUrl, boolean isActive, String title, ApplicationEventPublisher eventPublisher) {
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            var generatedShortUrl = shortUrlGenerator.generate();
            var linkToSave = linkMapper.toLink(userId, longUrl, generatedShortUrl, isActive, false, title);

            try {
                var savedLink = transactionTemplate.execute( status -> {
                    var result = linkRepository.save(linkToSave);
                    eventPublisher.publishEvent(new LinkCreatedApplicationEvent(result));

                    return result;
                });

                Objects.requireNonNull(savedLink, "Transaction completed but returned a null Link");

                return linkMapper.toCreateLinkResponse(savedLink);
            } catch (DataIntegrityViolationException e) {
                // A collision occurred due to the race condition.
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
