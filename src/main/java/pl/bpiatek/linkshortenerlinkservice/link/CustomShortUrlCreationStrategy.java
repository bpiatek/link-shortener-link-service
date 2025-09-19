package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

class CustomShortUrlCreationStrategy implements  LinkCreationStrategy {

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;

    CustomShortUrlCreationStrategy(LinkRepository linkRepository, LinkMapper linkMapper) {
        this.linkRepository = linkRepository;
        this.linkMapper = linkMapper;
    }

    @Override
    public CreateLinkResponse createLink(String userId, String longUrl, String shortUrl) {
        try {
            var linkToSave = linkMapper.toLink(userId, longUrl, shortUrl);
            var savedLink = linkRepository.save(linkToSave);
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
