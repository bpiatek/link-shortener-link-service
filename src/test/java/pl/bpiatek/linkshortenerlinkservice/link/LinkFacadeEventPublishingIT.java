package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.bpiatek.linkshortenerlinkservice.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class LinkFacadeEventPublishingIT extends IntegrationTest {

    @Autowired
    private LinkFacade linkFacade;

    @Autowired
    private LinkFixtures linkFixtures;

    @Test
    void shouldNotPublishEventWhenDatabaseSaveFails() {
        // given
        var link = linkFixtures.aLink();

        // then
        assertThatThrownBy(() ->
                linkFacade.createLink("user-123", "https://example.com", link.shortUrl(), true, null))
                .isInstanceOf(RuntimeException.class);

        //TODO check if event was not sent
    }
}