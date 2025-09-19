package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

class LinkStubs {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    private LinkStubs() {
    }

    static Link aLinkWithShortUrl(String shortUrl) {
        return new Link(null, USER_ID, shortUrl, LONG_URL, null, null, true, null, null, null);
    }

    static Link aSavedLinkWithShortUrl(long id, String shortUrl) {
        return new Link(id, USER_ID, shortUrl, LONG_URL, null, null, true, null, null, null);
    }

    static CreateLinkResponse aCreateLinkResponseWithShortUrl(String shortUrl) {
        return new CreateLinkResponse("http://base.url/" + shortUrl, LONG_URL);
    }

    static CreateLinkResponse aCreateLinkResponseWithLongUrl(String longUrl) {
        return new CreateLinkResponse("http://base.url/24341", longUrl);
    }
}
