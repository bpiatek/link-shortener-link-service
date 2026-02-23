package pl.bpiatek.linkshortenerlinkservice.link;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.exception.UnableToGenerateUniqueShortUrlException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aCreateLinkResponseWithShortUrl;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aLinkWithShortUrl;

@ExtendWith(MockitoExtension.class)
class RandomShortUrlCreationStrategyTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";
    private static final String TITLE = "Test title";

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ShortUrlGenerator shortUrlGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LinkTransactionalPersister linkTransactionalPersister;

    private RandomShortUrlCreationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RandomShortUrlCreationStrategy(linkMapper, shortUrlGenerator, linkTransactionalPersister);
    }

    @Test
    void shouldSucceedOnFirstAttempt() {
        // given
        var uniqueShortUrl = "abc1234";
        givenGeneratorReturns(uniqueShortUrl);
        var expectedResponse = givenSuccessfulPersist(uniqueShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, null, true, TITLE, eventPublisher);

        // then
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(shortUrlGenerator).generate();

        verify(linkTransactionalPersister).persistAndPublish(any(Link.class), eq(eventPublisher));
    }

    @Test
    void shouldRetryAndSucceedOnCollision() {
        var collidingShortUrl = "colliding";
        var uniqueShortUrl = "unique123";

        givenGeneratorReturns(collidingShortUrl, uniqueShortUrl);
        givenCollisionOnPersist(collidingShortUrl);
        var expectedResponse = givenSuccessfulPersist(uniqueShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, null, true, TITLE, eventPublisher);

        // then:
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(shortUrlGenerator, times(2)).generate();
        verify(linkTransactionalPersister, times(2)).persistAndPublish(any(Link.class), eq(eventPublisher));
    }

    @Test
    void shouldThrowExceptionWhenAllAttemptsCollide() {
        // given
        var collidingShortUrl = "always-colliding";
        givenGeneratorReturns(collidingShortUrl, collidingShortUrl, collidingShortUrl, collidingShortUrl, collidingShortUrl);
        givenCollisionOnPersist(collidingShortUrl);

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, null, false, "title", eventPublisher))
                .isInstanceOf(UnableToGenerateUniqueShortUrlException.class);

        verify(shortUrlGenerator, times(5)).generate();
        verify(linkTransactionalPersister, times(5)).persistAndPublish(any(Link.class), eq(eventPublisher));
    }

    private void givenGeneratorReturns(String firstShortUrl, String... subsequentShortUrls) {
        given(shortUrlGenerator.generate()).willReturn(firstShortUrl, subsequentShortUrls);
    }

    private void givenCollisionOnPersist(String collidingShortUrl) {
        var link = aLinkWithShortUrl(collidingShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(collidingShortUrl), anyBoolean(), anyBoolean(), anyString()))
                .willReturn(link);

        given(linkTransactionalPersister.persistAndPublish(eq(link), eq(eventPublisher)))
                .willThrow(new DataIntegrityViolationException("Collision!"));
    }

    private CreateLinkResponse givenSuccessfulPersist(String uniqueShortUrl) {
        var linkToSave = aLinkWithShortUrl(uniqueShortUrl);
        var expectedResponse = aCreateLinkResponseWithShortUrl(uniqueShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(uniqueShortUrl), anyBoolean(), anyBoolean(), anyString()))
                .willReturn(linkToSave);

        given(linkTransactionalPersister.persistAndPublish(eq(linkToSave), eq(eventPublisher)))
                .willReturn(expectedResponse);

        return expectedResponse;
    }
}