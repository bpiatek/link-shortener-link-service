package pl.bpiatek.linkshortenerlinkservice.link;

import java.time.Instant;

record Link(
        Long id,
        String userId,
        String shortUrl,
        String longUrl,
        String title,
        String notes,
        boolean isActive,
        boolean isCustom,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {
    Link withIdAndCreatedAt(Long generatedId, Instant createdAt) {
        return new Link(
                generatedId, this.userId, this.shortUrl, this.longUrl, this.title,
                this.notes, this.isActive, this.isCustom, createdAt, this.updatedAt, this.expiresAt
        );
    }

    Link(String userId, String shortUrl, String longUrl, boolean isActive, boolean isCustom, String title) {
        this(null, userId, shortUrl, longUrl, title, null, isActive, isCustom, null, null, null);
    }

    static Link empty() {
        return new Link(null, null, null, null, null, null, false, false, null, null, null);
    }
}
