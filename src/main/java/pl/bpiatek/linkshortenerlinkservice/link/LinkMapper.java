package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

class LinkMapper {

    private final String baseLinkUrl;

    LinkMapper(String baseLinkUrl) {
        this.baseLinkUrl = baseLinkUrl;
    }

    CreateLinkResponse toCreateLinkResponse(Link link) {
        return new CreateLinkResponse(baseLinkUrl + link.shortUrl(), link.longUrl());
    }

    Link toLink(String userId, String longUrl, String shortUrl, boolean isActive) {
        return new Link(userId, shortUrl, longUrl, isActive);
    }

}
