package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;
import pl.bpiatek.linkshortenerlinkservice.exception.LinkNotFoundException;

class LinkRetriever {

    private final LinkRepository linkRepository;
    private final LinkMapper linkMapper;

    LinkRetriever(LinkRepository linkRepository, LinkMapper linkMapper) {
        this.linkRepository = linkRepository;
        this.linkMapper = linkMapper;
    }

    LinkDto getLink(String userId, Long linkId) {
        return linkRepository.findByIdAndUserId(linkId, userId)
                .map(linkMapper::toLinkDto)
                .orElseThrow(() -> new LinkNotFoundException("Link not found or access denied"));
    }
}
