package pl.bpiatek.linkshortenerlinkservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import pl.bpiatek.linkshortenerlinkservice.api.ValidUrl;

public record CreateLinkRequest(
        @NotBlank(message = "The destination URL cannot be blank.")
        @ValidUrl(message = "A valid URL format is required.")
        String longUrl,
        String shortUrl) {
}
