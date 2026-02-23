package pl.bpiatek.linkshortenerlinkservice.link;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.ReservedShortUrlException;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aCreateLinkResponseWithShortUrl;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aLinkWithShortUrl;

@ExtendWith(MockitoExtension.class)
class CustomShortUrlCreationStrategyTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";
    private static final String TITLE = "test title";

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LinkTransactionalPersister linkTransactionalPersister;

    private CustomShortUrlCreationStrategy strategy;

    @BeforeEach
    void setUp() {
        var reservedWordsValidator = new ReservedWordsValidator(Set.of("dashboard", "login"));
        strategy = new CustomShortUrlCreationStrategy(linkMapper, reservedWordsValidator, linkTransactionalPersister);
    }

    @Test
    void shouldSucceedCreatingLink() {
        // given
        var customShortUrl = "test-url";
        var expectedResponse = givenSuccessfulPersist(customShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, customShortUrl, true, TITLE, eventPublisher);

        // then
        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(linkTransactionalPersister).persistAndPublish(any(Link.class), eq(eventPublisher));
    }

    @Test
    void shouldFailOnCollision() {
        // given
        var customShortUrl = "colliding-url";
        givenCollisionOnPersist(customShortUrl);

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, customShortUrl, true, TITLE, eventPublisher))
                .isInstanceOf(ShortCodeAlreadyExistsException.class);

        verify(linkTransactionalPersister).persistAndPublish(any(Link.class), eq(eventPublisher));
    }

    @Test
    void shouldThrowExceptionWhenReserveShortUrlProvided() {
        // given
        var reservedShortUrl = "dashboard";

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, reservedShortUrl, true, TITLE, eventPublisher))
                .isInstanceOf(ReservedShortUrlException.class)
                .hasMessageContaining("Short URL 'dashboard' is reserved and cannot be used.");
    }

    @Test
    void shouldThrowExceptionWhenReserveShortUrlWithSlashProvided() {
        // given
        var reservedShortUrl = "dashboard/links/123/edit";

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, reservedShortUrl, true, TITLE, eventPublisher))
                .isInstanceOf(ReservedShortUrlException.class)
                .hasMessageContaining("Short URL 'dashboard/links/123/edit' is reserved and cannot be used.");
    }

    private CreateLinkResponse givenSuccessfulPersist(String customShortUrl) {
        var linkToSave = aLinkWithShortUrl(customShortUrl);
        var expectedResponse = aCreateLinkResponseWithShortUrl(customShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(customShortUrl), anyBoolean(), anyBoolean(), anyString()))
                .willReturn(linkToSave);

        given(linkTransactionalPersister.persistAndPublish(eq(linkToSave), eq(eventPublisher)))
                .willReturn(expectedResponse);

        return expectedResponse;
    }

    private void givenCollisionOnPersist(String collidingShortUrl) {
        var link = aLinkWithShortUrl(collidingShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(collidingShortUrl), anyBoolean(), anyBoolean(), anyString()))
                .willReturn(link);

        given(linkTransactionalPersister.persistAndPublish(eq(link), eq(eventPublisher)))
                .willThrow(new DataIntegrityViolationException("Collision!"));
    }
}
