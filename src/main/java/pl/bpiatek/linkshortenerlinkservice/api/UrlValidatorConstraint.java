package pl.bpiatek.linkshortenerlinkservice.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.UrlValidator;

class UrlValidatorConstraint implements ConstraintValidator<ValidUrl, String> {

    private static final UrlValidator validator = new UrlValidator(new String[]{"http", "https", "ftp"});

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (validator.isValid(value)) {
            return true;
        }

        return validator.isValid("https://" + value);
    }
}
