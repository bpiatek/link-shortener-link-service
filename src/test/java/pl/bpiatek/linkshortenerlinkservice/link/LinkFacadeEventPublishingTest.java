package pl.bpiatek.linkshortenerlinkservice.link;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = LinkConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@ActiveProfiles("test")
class LinkFacadeEventPublishingTest {

    @Autowired
    private LinkFacade linkFacade;

    @MockitoBean
    private LinkRepository linkRepository;

    @MockitoBean
    private LinkCreatedKafkaProducer linkCreatedKafkaProducer;

    @MockitoBean
    private LinkFixtures linkFixtures;

    @Test
    void shouldNotPublishEventWhenDatabaseSaveFails() {
        // given
        given(linkRepository.save(any(Link.class)))
                .willThrow(new DataIntegrityViolationException("Simulated database error"));

        // then
        assertThatThrownBy(() ->
                linkFacade.createLink("user-123", "https://example.com", "any-code", true, null))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(linkCreatedKafkaProducer);
    }
}