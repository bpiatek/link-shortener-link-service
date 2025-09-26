package pl.bpiatek.linkshortenerlinkservice.exception;

public class ShortCodeAlreadyExistsException extends RuntimeException {

    public ShortCodeAlreadyExistsException(String shortUrl) {
        super("Short link with provided shortUrl '" + shortUrl + "' already exists.");
    }
}
