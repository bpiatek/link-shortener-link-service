package pl.bpiatek.linkshortenerlinkservice.link;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aCreateLinkResponseWithShortUrl;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aLinkWithShortUrl;
import static pl.bpiatek.linkshortenerlinkservice.link.LinkStubs.aSavedLinkWithShortUrl;

@ExtendWith(MockitoExtension.class)
class CustomShortUrlCreationStrategyTest {

    private static final String LONG_URL = "https://example.com/long";
    private static final String USER_ID = "123";

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkMapper linkMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CustomShortUrlCreationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CustomShortUrlCreationStrategy(linkRepository, linkMapper);
    }

    @Test
    void shouldSucceedCreatingLink() {
        // given
        var customShortUrl = "test-url";
        var savedLink = givenSuccessfulSave(customShortUrl);

        // when
        var actualResponse = strategy.createLink(USER_ID, LONG_URL, customShortUrl, eventPublisher);

        // then
        assertThat(actualResponse.shortUrl()).contains(customShortUrl);
        var eventCaptor = ArgumentCaptor.forClass(LinkCreatedApplicationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().link()).isEqualTo(savedLink);
    }

    @Test
    void shouldFailOnCollision() {
        // given
        var customShortUrl = "colliding-url";
        givenCollisionOnSave(customShortUrl);

        // then
        assertThatThrownBy(() -> strategy.createLink(USER_ID, LONG_URL, customShortUrl, eventPublisher))
                .isInstanceOf(ShortCodeAlreadyExistsException.class);
        verify(linkRepository).save(any(Link.class));
    }

    private Link givenSuccessfulSave(String uniqueShortUrl) {
        var linkToSave = aLinkWithShortUrl(uniqueShortUrl);
        var savedLink = aSavedLinkWithShortUrl(1L, uniqueShortUrl);
        var response = aCreateLinkResponseWithShortUrl(uniqueShortUrl);

        given(linkMapper.toLink(anyString(), anyString(), eq(uniqueShortUrl))).willReturn(linkToSave);
        given(linkRepository.save(linkToSave)).willReturn(savedLink);
        given(linkMapper.toCreateLinkResponse(savedLink)).willReturn(response);

        return savedLink;
    }

    private void givenCollisionOnSave(String collidingShortUrl) {
        var link = aLinkWithShortUrl(collidingShortUrl);
        given(linkMapper.toLink(anyString(), anyString(), eq(collidingShortUrl))).willReturn(link);
        given(linkRepository.save(link)).willThrow(new DataIntegrityViolationException("Collision!"));
    }
}