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
    private static final String USER_ID = "123";
    private static final Boolean IS_ACTIVE = null;
    private static final String TITLE = "title";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkFacade linkFacade;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateLinkAndReturn201CreatedWhenRequestIsValid() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, "custom", IS_ACTIVE, TITLE);

        var facadeResponse = new CreateLinkResponse("custom", LONG_URL);
        when(linkFacade.createLink(USER_ID, request.longUrl(), request.shortUrl(), request.isActive(), request.title()))
                .thenReturn(facadeResponse);

        // when
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortUrl", is(facadeResponse.shortUrl())))
                .andExpect(jsonPath("$.longUrl", is(facadeResponse.longUrl())));

        // then
        verify(linkFacade).createLink(USER_ID, request.longUrl(), request.shortUrl(), request.isActive(), request.title());
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenLongUrlIsInvalid() throws Exception {
        // given
        var request = new CreateLinkRequest("invalid-url", null, IS_ACTIVE, null);

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403ForbiddenWhileCreatingLinkWhenUSER_IdHeaderIsMissing() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, null, IS_ACTIVE, null);

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
        var request = new CreateLinkRequest("", null, IS_ACTIVE, null);

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
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
        var shortUrl = "taken";
        var request = new CreateLinkRequest(LONG_URL, shortUrl, IS_ACTIVE, null);

        when(linkFacade.createLink(USER_ID, request.longUrl(), request.shortUrl(), request.isActive(), request.title()))
                .thenThrow(new ShortCodeAlreadyExistsException(shortUrl));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Shortening link failed")))
                .andExpect(jsonPath("$.detail", containsString(shortUrl)));
    }

    @Test
    void shouldReturn415UnsupportedMediaTypeWhileCreatingLinkWhenContentTypeIsNotApplicationJson() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, "my-code", IS_ACTIVE, TITLE);

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.TEXT_PLAIN) // Incorrect content type
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturn503ServiceUnavailableWhileCreatingLinkWhenFacadeThrowsUnableToGenerateUniqueShortUrlException() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, null, IS_ACTIVE, TITLE);

        when(linkFacade.createLink(USER_ID, request.longUrl(), request.shortUrl(), request.isActive(), request.title()))
                .thenThrow(new UnableToGenerateUniqueShortUrlException(5));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title", is("Shortening link failed")))
                .andExpect(jsonPath("$.detail",
                        is("The service is temporarily unable to generate a new link. Please try again later.")));
    }

    @Test
    void shouldReturn500InternalServerErrorWhileCreatingLinkWhenFacadeThrowsUnexpectedException() throws Exception {
        // given
        var request = new CreateLinkRequest(LONG_URL, "my-code", IS_ACTIVE, null);

        when(linkFacade.createLink(USER_ID, request.longUrl(), request.shortUrl(), request.isActive(), request.title()))
                .thenThrow(new RuntimeException("A critical database error occurred!"));

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred.")));
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenRequestBodyIsMissing() throws Exception {
        // given
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                // .content()  no content
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Malformed Request")))
                .andExpect(jsonPath("$.detail", is("The request body is missing or cannot be parsed.")));
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenRequestBodyIsMalformedJson() throws Exception {
        // given
        var malformedJson = "{\"longUrl\": \"https://example.com\", }"; // Extra comma makes it invalid

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Malformed Request")))
                .andExpect(jsonPath("$.detail", is("The request body is missing or cannot be parsed.")));
    }

    @Test
    void shouldReturn400BadRequestWhileCreatingLinkWhenLongLinkIsNotCorrectlyFormatted() throws Exception {
        var request = new CreateLinkRequest("https:// example.com", "my-code", IS_ACTIVE, TITLE);

        // then
        mockMvc.perform(post("/links")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Failed")))
                .andExpect(jsonPath("$.errors[0].message", is("A valid URL format is required.")))
                .andExpect(jsonPath("$.errors[0].field", is("longUrl")));
    }

    @TestConfiguration
    static class ControllerTestConfig {

        @Bean
        public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
            return builder.build();
        }
    }
}