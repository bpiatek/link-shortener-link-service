package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import java.util.Objects;

class CustomShortUrlCreationStrategy implements LinkCreationStrategy {

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final ReservedWordsValidator reservedWordsValidator;
    private final TransactionTemplate transactionTemplate;


    CustomShortUrlCreationStrategy(LinkRepository linkRepository, LinkMapper linkMapper, ReservedWordsValidator reservedWordsValidator, TransactionTemplate transactionTemplate) {
        this.linkRepository = linkRepository;
        this.linkMapper = linkMapper;
        this.reservedWordsValidator = reservedWordsValidator;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl, boolean isActive, String title, ApplicationEventPublisher eventPublisher) {
        reservedWordsValidator.validate(shortUrl);
        var linkToSave = linkMapper.toLink(userId, longUrl, shortUrl, isActive, true, title);

        try {
            var savedLink = transactionTemplate.execute(status -> {
                var result = linkRepository.save(linkToSave);
                eventPublisher.publishEvent(new LinkCreatedApplicationEvent(result));

                return result;
            });

            Objects.requireNonNull(savedLink, "Transaction completed but returned a null Link");

            return linkMapper.toCreateLinkResponse(savedLink);
        } catch (DataIntegrityViolationException e) {
            throw new ShortCodeAlreadyExistsException(shortUrl);
        }
    }

    @Override
    public CreationStrategyType getType() {
        return CreationStrategyType.CUSTOM;
    }
}
