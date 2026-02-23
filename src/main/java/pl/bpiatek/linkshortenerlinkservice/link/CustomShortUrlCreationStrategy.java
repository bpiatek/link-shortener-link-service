package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

class CustomShortUrlCreationStrategy implements LinkCreationStrategy {

    private final LinkMapper linkMapper;
    private final ReservedWordsValidator reservedWordsValidator;
    private final LinkTransactionalPersister linkTransactionalPersister;

    CustomShortUrlCreationStrategy(LinkMapper linkMapper, ReservedWordsValidator reservedWordsValidator, LinkTransactionalPersister linkTransactionalPersister) {
        this.linkMapper = linkMapper;
        this.reservedWordsValidator = reservedWordsValidator;
        this.linkTransactionalPersister = linkTransactionalPersister;
    }


    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl, boolean isActive, String title, ApplicationEventPublisher eventPublisher) {
        reservedWordsValidator.validate(shortUrl);
        var linkToSave = linkMapper.toLink(userId, longUrl, shortUrl, isActive, true, title);

        try {
            return linkTransactionalPersister.persistAndPublish(linkToSave, eventPublisher);
        } catch (DataIntegrityViolationException e) {
            throw new ShortCodeAlreadyExistsException(shortUrl);
        }
    }

    @Override
    public CreationStrategyType getType() {
        return CreationStrategyType.CUSTOM;
    }
}
