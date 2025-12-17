package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.api.dto.LinkDto;

class LinkMapper {

    private final String baseLinkUrl;

    LinkMapper(String baseLinkUrl) {
        this.baseLinkUrl = baseLinkUrl;
    }

    CreateLinkResponse toCreateLinkResponse(Link link) {
        return new CreateLinkResponse(baseLinkUrl + link.shortUrl(), link.longUrl());
    }

    Link toLink(String userId, String longUrl, String shortUrl, boolean isActive, String title) {
        return new Link(userId, shortUrl, longUrl, isActive, title);
    }

    LinkDto toLinkDto(Link link) {
        return new LinkDto(
                String.valueOf(link.id()),
                link.userId(),
                link.shortUrl(),
                link.longUrl(),
                link.title(),
                link.notes(),
                link.isActive(),
                link.createdAt(),
                link.updatedAt(),
                link.expiresAt()
        );
    }
}
