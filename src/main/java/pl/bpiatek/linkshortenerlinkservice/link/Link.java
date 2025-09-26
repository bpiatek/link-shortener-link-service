package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

record Link(
        Long id,
        String userId,
        String shortUrl,
        String longUrl,
        String title,
        String notes,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {
    Link withId(Long generatedId) {
        return new Link(
                generatedId, this.userId, this.shortUrl, this.longUrl, this.title,
                this.notes, this.isActive, this.createdAt, this.updatedAt, this.expiresAt
        );
    }

    Link(String userId, String shortUrl, String longUrl, boolean isActive) {
        this(null, userId, shortUrl, longUrl, null, null, isActive, null, null, null);
    }

    static Link empty() {
        return new Link(null, null, null, null, null, null, false, null, null, null);
    }
}