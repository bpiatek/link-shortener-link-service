package pl.bpiatek.linkshortenerlinkservice.exception;

public class UnableToGenerateUniqueShortUrlException extends RuntimeException {

    public UnableToGenerateUniqueShortUrlException(int maxAttempts) {
        super("Failed to generate a unique short code after " + maxAttempts + " attempts.");
    }
}
