package pl.bpiatek.linkshortenerlinkservice.link;

import java.security.SecureRandom;

class ShortUrlGenerator {

    private static final String ALPHANUMERIC_CHARS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private static final SecureRandom random = new SecureRandom();

    private final int urlLength;

    public ShortUrlGenerator(int urlLength) {
        this.urlLength = urlLength;
    }

    String generate() {
        StringBuilder sb = new StringBuilder(urlLength);
        for (int i = 0; i < urlLength; i++) {
            int randomIndex = random.nextInt(ALPHANUMERIC_CHARS.length());
            char randomChar = ALPHANUMERIC_CHARS.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }
}
