package pl.bpiatek.linkshortenerlinkservice.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkRequest;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;
import pl.bpiatek.linkshortenerlinkservice.config.ClockConfiguration;
import pl.bpiatek.linkshortenerlinkservice.config.TestSecurityConfiguration;
import pl.bpiatek.linkshortenerlinkservice.exception.ShortCodeAlreadyExistsException;
import pl.bpiatek.linkshortenerlinkservice.exception.UnableToGenerateUniqueShortUrlException;
import pl.bpiatek.linkshortenerlinkservice.link.LinkFacade;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(LinkController.class)
@Import({TestSecurityConfiguration.class, ClockConfiguration.class})
@ActiveProfiles("test")
class LinkControllerTest {

    private static final String LONG_URL = "https://example.com/long";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkFacade linkFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateLinkAndReturn201CreatedWhenRequestIsValid() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, "my-custom-code");
        var userId = "user-123";

        var facadeResponse = new CreateLinkResponse("https://apidev.bpiatek.pl/my-custom-code", LONG_URL);
        when(linkFacade.createLink(userId, request.longUrl(), request.shortUrl()))
                .thenReturn(facadeResponse);

        // when
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortUrl", is(facadeResponse.shortUrl())))
                .andExpect(jsonPath("$.longUrl", is(facadeResponse.longUrl())));

        // then
        verify(linkFacade).createLink(userId, request.longUrl(), request.shortUrl());
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenLongUrlIsInvalid() throws Exception {
        // given
        var request = new CreateLinkRequest("invalid-url", null);
        var userId = "user-123";

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403ForbiddenWhileCreatingLinkWhenUserIdHeaderIsMissing() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, null);

        // then
        mockMvc.perform(post("/links")
                        // .header("X-User-Id", "user-123") // Header is intentionally omitted
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenLongUrlIsBlank() throws Exception {
        // given
        var request = new CreateLinkRequest("", null);
        var userId = "user-123";

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.errors[0].message", is("The destination URL cannot be blank.")))
                .andExpect(jsonPath("$.errors[0].field", is("longUrl")));
    }

    @Test
    void shouldReturn409ConflictWhileCreatingLinkWhenFacadeThrowsShortCodeAlreadyExistsException() throws Exception {
        // given
        var shortUrl = "already-taken";
        var request = new CreateLinkRequest(LONG_URL, shortUrl);
        var userId = "user-123";

        when(linkFacade.createLink(userId, request.longUrl(), request.shortUrl()))
                .thenThrow(new ShortCodeAlreadyExistsException(shortUrl));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Shortening link failed")))
                .andExpect(jsonPath("$.detail", containsString(shortUrl)));
    }

    @Test
    void shouldReturn415UnsupportedMediaTypeWhileCreatingLinkWhenContentTypeIsNotApplicationJson() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, "my-code");
        var userId = "user-123";

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.TEXT_PLAIN) // Incorrect content type
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturn503ServiceUnavailableWhileCreatingLinkWhenFacadeThrowsUnableToGenerateUniqueShortUrlException() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, null);
        var userId = "user-123";

        when(linkFacade.createLink(userId, request.longUrl(), request.shortUrl()))
                .thenThrow(new UnableToGenerateUniqueShortUrlException(5));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title", is("Shortening link failed")))
                .andExpect(jsonPath("$.detail",
                        is("The service is temporarily unable to generate a new link. Please try again later.")));
    }

    @Test
    void shouldReturn500InternalServerErrorWhileCreatingLinkWhenFacadeThrowsUnexpectedException() throws Exception {
        // given: A valid request.
        var request = new CreateLinkRequest(LONG_URL, "my-code");
        var userId = "user-123";

        when(linkFacade.createLink(userId, request.longUrl(), request.shortUrl()))
                .thenThrow(new RuntimeException("A critical database error occurred!"));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred.")));
    }

    @TestConfiguration
    static class ControllerTestConfig {

        @Bean
        public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
            return builder.build();
        }
    }
}