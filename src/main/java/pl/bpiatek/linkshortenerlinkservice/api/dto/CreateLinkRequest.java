package pl.bpiatek.linkshortenerlinkservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.bpiatek.linkshortenerlinkservice.api.ValidUrl;

public record CreateLinkRequest(
        @NotBlank(message = "The destination URL cannot be blank.")
        @ValidUrl(message = "A valid URL format is required.")
        String longUrl,
        @Size(max = 7, message = "Custom short URL cannot be longer than {max} characters.")
        String shortUrl,
        Boolean isActive) {
        @Override

        public Boolean isActive() {
                return this.isActive == null || isActive;
        }
}
