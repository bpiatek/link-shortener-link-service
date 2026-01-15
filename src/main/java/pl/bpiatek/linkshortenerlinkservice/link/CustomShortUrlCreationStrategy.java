package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

class CustomShortUrlCreationStrategy implements LinkCreationStrategy {

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;
    private final ReservedWordsValidator reservedWordsValidator;

    CustomShortUrlCreationStrategy(LinkRepository linkRepository, LinkMapper linkMapper, ReservedWordsValidator reservedWordsValidator) {
        this.linkRepository = linkRepository;
        this.linkMapper = linkMapper;
        this.reservedWordsValidator = reservedWordsValidator;
    }

    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl, boolean isActive, String title, ApplicationEventPublisher eventPublisher) {
        reservedWordsValidator.validate(shortUrl);
        try {
            var linkToSave = linkMapper.toLink(userId, longUrl, shortUrl, isActive, true, title);
            var savedLink = linkRepository.save(linkToSave);

            eventPublisher.publishEvent(new LinkCreatedApplicationEvent(savedLink));

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
