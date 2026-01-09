package pl.bpiatek.linkshortenerlinkservice.exception;

public class ReservedShortUrlException extends RuntimeException {
    public ReservedShortUrlException(String shortUrl) {
        super("Short URL '" + shortUrl + "' is reserved and cannot be used.");
    }
}
