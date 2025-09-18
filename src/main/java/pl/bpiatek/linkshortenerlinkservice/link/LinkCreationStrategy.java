package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

interface LinkCreationStrategy {
    CreateLinkResponse createLink(String userId, String longUrl, String shortUrl);
    CreationStrategyType getType();
}
