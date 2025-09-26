package pl.bpiatek.linkshortenerlinkservice.link;

import java.time.LocalDateTime;

public class TestLink {
    private final Long id;
    private final String userId;
    private final String shortUrl;
    private final String longUrl;
    private final String title;
    private final String notes;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime expiredAt;

    private TestLink(TestLinkBuilder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.shortUrl = builder.shortUrl;
        this.longUrl = builder.longUrl;
        this.title = builder.title;
        this.notes = builder.notes;
        this.isActive = builder.isActive;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.expiredAt = builder.expiredAt;
    }

    public static TestLinkBuilder builder() {
        return new TestLinkBuilder();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public static class TestLinkBuilder {
        private Long id;
        private String userId = "123";
        private String shortUrl = "aB5xZ1";
        private String longUrl = "https://example.com/a-very-long-url";
        private String title = "Example Title";
        private String notes = "Some notes";
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime expiredAt;

        public TestLinkBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TestLinkBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public TestLinkBuilder shortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
            return this;
        }

        public TestLinkBuilder longUrl(String longUrl) {
            this.longUrl = longUrl;
            return this;
        }

        public TestLinkBuilder title(String title) {
            this.title = title;
            return this;
        }

        public TestLinkBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public TestLinkBuilder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public TestLinkBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TestLinkBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public TestLinkBuilder expiredAt(LocalDateTime expiredAt) {
            this.expiredAt = expiredAt;
            return this;
        }

        public TestLink build() {
            return new TestLink(this);
        }
    }
}
