package pl.bpiatek.linkshortenerlinkservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(
        @NotBlank(message = "The destination URL cannot be blank.")
        @URL(message = "A valid URL format is required.")
        String longUrl,
        String shortUrl) {
}
