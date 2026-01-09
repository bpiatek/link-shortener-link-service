package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

import java.time.Instant;

import static java.time.Instant.parse;

class LinkStubs {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";
    private static final String SHORT_URL = "3ehnsT8";
    private static final String TITLE = "test title";

    private LinkStubs() {
    }

    static Link aLink() {
        return  new Link(1L, USER_ID, SHORT_URL, LONG_URL, TITLE, null, true,
                parse("2025-11-01T12:00:00Z"), parse("2025-11-02T13:00:00Z"), parse("2025-11-07T11:00:00Z"));
    }

    static Link aLinkWithShortUrl(String shortUrl) {
        return new Link(null, USER_ID, shortUrl, LONG_URL, TITLE, null, true,
                parse("2025-11-01T12:00:00Z"), parse("2025-11-02T13:00:00Z"), parse("2025-11-07T11:00:00Z"));
    }

    static Link aSavedLinkWithShortUrl(long id, String shortUrl) {
        return new Link(id, USER_ID, shortUrl, LONG_URL, TITLE, null, true,
                parse("2025-11-01T12:00:00Z"), parse("2025-11-02T13:00:00Z"), parse("2025-11-07T11:00:00Z"));
    }

    static CreateLinkResponse aCreateLinkResponseWithShortUrl(String shortUrl) {
        return new CreateLinkResponse("http://base.url/" + shortUrl, LONG_URL);
    }

    static CreateLinkResponse aCreateLinkResponseWithLongUrl(String longUrl) {
        return new CreateLinkResponse(SHORT_URL, longUrl);
    }
}
