package pl.bpiatek.linkshortenerlinkservice.api;


import jakarta.validation.ConstraintValidatorContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UrlValidatorConstraintTest {

    private final UrlValidatorConstraint validator = new UrlValidatorConstraint();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @ParameterizedTest(name = "URL: \"{0}\" should be valid")
    @ValueSource(strings = {
            "www.google.com",
            "http://google.com",
            "https://www.google.com",
            "https://bpiatek.pl/some/path",
            "https://www.youtube.com/watch?v=E2btdGWn2RE",
            "http://example.com:8080/path?query=value#fragment",
            "ftp://files.example.com/resource.zip"
    })
    void shouldReturnTrueForValidUrls(String validUrl) {
        // when
        boolean isValid = validator.isValid(validUrl, context);

        // then
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest(name = "URL: \"{0}\" should be invalid")
    @ValueSource(strings = {
            "https://www./watch?v=E2btdGWn2RE",
            "dupa",
            "htp://invalid.com",       // Invalid scheme
            "https://",                // Scheme only
            "https:// example.com",    // Contains spaces
            "https://.com"             // Missing domain
    })
    void shouldReturnFalseForInvalidUrls(String invalidUrl) {
        // when
        boolean isValid = validator.isValid(invalidUrl, context);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnTrueForNullAndBlankValues() {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(validator.isValid(null, context)).isTrue();
            s.assertThat(validator.isValid("", context)).isTrue();
            s.assertThat(validator.isValid("   ", context)).isTrue();
        });
    }
}