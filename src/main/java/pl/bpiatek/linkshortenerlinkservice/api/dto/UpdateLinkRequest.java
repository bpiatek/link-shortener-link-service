package pl.bpiatek.linkshortenerlinkservice.api.dto;

import jakarta.validation.constraints.Size;
import pl.bpiatek.linkshortenerlinkservice.api.ValidUrl;

public record UpdateLinkRequest(
        @ValidUrl(message = "A valid URL format is required.")
        @Size(max = 1024, message = "The destination URL cannot exceed 1024 characters.")
        String longUrl,

        Boolean isActive,

        @Size(max = 255, message = "The title cannot exceed 255 characters.")
        String title
) {
}
