package pl.bpiatek.linkshortenerlinkservice.api.dto;

import java.time.Instant;

public record LinkDto(
        String id,
        String userId,
        String shortUrl,
        String longUrl,
        String title,
        String notes,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {
}
