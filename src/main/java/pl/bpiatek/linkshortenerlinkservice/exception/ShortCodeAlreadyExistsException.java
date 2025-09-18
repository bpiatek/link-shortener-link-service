package pl.bpiatek.linkshortenerlinkservice.exception;

public class ShortCodeAlreadyExistsException extends RuntimeException {

    public ShortCodeAlreadyExistsException(String shortUrl) {
        super("Short ink with provided shortUrl '" + shortUrl + "' already exists.");
    }
}
