package pl.bpiatek.linkshortenerlinkservice.link;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aSavedLinkWithShortUrl;

@ExtendWith(MockitoExtension.class)
class RandomShortUrlCreationStrategyTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ShortUrlGenerator shortUrlGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RandomShortUrlCreationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RandomShortUrlCreationStrategy(linkRepository, linkMapper, shortUrlGenerator);
    }

    @Test
    void shouldSucceedOnFirstAttempt() {
        // given
        var uniqueShortUrl = "abc1234";
        givenGeneratorReturns(uniqueShortUrl);
        var savedLink = givenSuccessfulSave(uniqueShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, null, true, eventPublisher);

        // then
        assertThat(actualResponse.shortUrl()).contains(uniqueShortUrl);
        verify(shortUrlGenerator).generate();
        verify(linkRepository).save(any(Link.class));
        var eventCaptor = ArgumentCaptor.forClass(LinkCreatedApplicationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().link()).isEqualTo(savedLink);
    }

    @Test
    void shouldRetryAndSucceedOnCollision() {
        var collidingShortUrl = "colliding";
        var uniqueShortUrl = "unique123";

        givenGeneratorReturns(collidingShortUrl, uniqueShortUrl);
        givenCollisionOnSave(collidingShortUrl);
        givenSuccessfulSave(uniqueShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, null, true, eventPublisher);

        // then:
        assertThat(actualResponse.shortUrl()).contains(uniqueShortUrl);
        verify(shortUrlGenerator, times(2)).generate();
        verify(linkRepository, times(2)).save(any(Link.class));
    }

    @Test
    void shouldThrowExceptionWhenAllAttemptsCollide() {
        // given
        var collidingShortUrl = "always-colliding";
        givenGeneratorReturns(collidingShortUrl, collidingShortUrl, collidingShortUrl, collidingShortUrl, collidingShortUrl);
        givenCollisionOnSave(collidingShortUrl);

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, null,false, eventPublisher))
                .isInstanceOf(UnableToGenerateUniqueShortUrlException.class);
        verify(shortUrlGenerator, times(5)).generate();
        verify(linkRepository, times(5)).save(any(Link.class));
    }

    private void givenGeneratorReturns(String firstShortUrl, String... subsequentShortUrls) {
        given(shortUrlGenerator.generate()).willReturn(firstShortUrl, subsequentShortUrls);
    }

    private void givenCollisionOnSave(String collidingShortUrl) {
        var link = aLinkWithShortUrl(collidingShortUrl);
        given(linkMapper.toLink(anyString(), anyString(), eq(collidingShortUrl), anyBoolean())).willReturn(link);
        given(linkRepository.save(link)).willThrow(new DataIntegrityViolationException("Collision!"));
    }

    private Link givenSuccessfulSave(String uniqueShortUrl) {
        var linkToSave = aLinkWithShortUrl(uniqueShortUrl);
        var savedLink = aSavedLinkWithShortUrl(1L, uniqueShortUrl);
        var response = aCreateLinkResponseWithShortUrl(uniqueShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(uniqueShortUrl), anyBoolean())).willReturn(linkToSave);
        given(linkRepository.save(linkToSave)).willReturn(savedLink);
        given(linkMapper.toCreateLinkResponse(savedLink)).willReturn(response);

        return savedLink;
    }
}