package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlGeneratorTest {

    private static final int URL_LENGTH = 7;

    @Test
    void shouldGenerateShortUrl() {
        // given
        var generator = new ShortUrlGenerator(URL_LENGTH);

        // when
        var shortUrl = generator.generate();

        // then
        assertThat(shortUrl).hasSize(URL_LENGTH);
        assertThat(shortUrl).doesNotContainAnyWhitespaces();
        assertThat(shortUrl).matches("[a-zA-Z0-9&&[^0OlI]]+");
    }
}