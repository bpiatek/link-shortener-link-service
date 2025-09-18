package pl.bpiatek.linkshortenerlinkservice.api.dto;

public record CreateLinkResponse(
        String shortUrl,
        String longUrl) {
}
