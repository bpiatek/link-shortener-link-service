package pl.bpiatek.linkshortenerlinkservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.bpiatek.linkshortenerlinkservice.api.ValidUrl;

public record CreateLinkRequest(
        @NotBlank(message = "The destination URL cannot be blank.")
        @ValidUrl(message = "A valid URL format is required.")
        @Size(max = 1024, message = "The destination URL cannot exceed 1024 characters.")
        String longUrl,
        @Pattern(regexp = "^$|^[a-zA-Z0-9-_]+$", message = "Short URL can only contain alphanumeric characters, hyphens, and underscores.")
        String shortUrl,
        Boolean isActive,
        @Size(max = 255, message = "The title cannot exceed 255 characters.")
        String title) {

        @Override
        public Boolean isActive() {
                return this.isActive == null || isActive;
        }
}
