package pl.bpiatek.linkshortenerlinkservice.link;

import pl.bpiatek.linkshortenerlinkservice.exception.ReservedShortUrlException;

import java.util.Set;

class ReservedWordsValidator {

    private final Set<String> reservedWords;

    ReservedWordsValidator(Set<String> reservedWords) {
        this.reservedWords = reservedWords;
    }

    void validate(String shortUrl) {
        if (shortUrl == null) {
            return;
        }
        
        var lowerCaseShortUrl = shortUrl.toLowerCase();
        
        for (var reservedWord : reservedWords) {
            var lowerReserved = reservedWord.toLowerCase();
            if (lowerCaseShortUrl.equals(lowerReserved) || lowerCaseShortUrl.startsWith(lowerReserved + "/")) {
                throw new ReservedShortUrlException(shortUrl);
            }
        }
    }
}
